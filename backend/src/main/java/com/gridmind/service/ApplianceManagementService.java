package com.gridmind.service;

import com.gridmind.dto.ApplianceDetailDto;
import com.gridmind.dto.FaultSimRequest;
import com.gridmind.dto.ResetResultDto;
import com.gridmind.dto.TelemetryBroadcastDto;
import com.gridmind.model.*;
import com.gridmind.repository.ApplianceNodeRepository;
import com.gridmind.repository.FaultEventLogRepository;
import com.gridmind.service.VirtualApplianceSimulatorService.ApplianceInternalState;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ApplianceManagementService {

    private final VirtualApplianceSimulatorService simulatorService;
    private final BaselineProfileService baselineProfileService;
    private final ApplianceNodeRepository applianceNodeRepository;
    private final FaultEventLogRepository faultEventLogRepository;

    public ApplianceManagementService(
            VirtualApplianceSimulatorService simulatorService,
            BaselineProfileService baselineProfileService,
            ApplianceNodeRepository applianceNodeRepository,
            FaultEventLogRepository faultEventLogRepository) {
        this.simulatorService = simulatorService;
        this.baselineProfileService = baselineProfileService;
        this.applianceNodeRepository = applianceNodeRepository;
        this.faultEventLogRepository = faultEventLogRepository;
    }

    public List<ApplianceNode> listAppliances() {
        return simulatorService.getApplianceNodes();
    }

    public List<TelemetryBroadcastDto> getLatestTelemetryAll() {
        return simulatorService.getLatestBroadcastSnapshot();
    }

    public ApplianceDetailDto getApplianceDetail(String applianceId) {
        ApplianceInternalState st = simulatorService.getInternalState(applianceId)
                .orElseThrow(() -> new NoSuchElementException("Appliance not found: " + applianceId));

        List<TelemetrySample> history;
        synchronized (st) {
            history = new ArrayList<>(st.history);
        }
        List<BaselineProfileDoc> baseline = baselineProfileService.getProfilesForAppliance(st.node.getApplianceType());
        return new ApplianceDetailDto(st.node, st.latestSample, history, baseline);
    }

    public ApplianceNode injectScenario(String applianceId, FaultSimRequest request) {
        ApplianceInternalState st = simulatorService.getInternalState(applianceId)
                .orElseThrow(() -> new NoSuchElementException("Appliance not found: " + applianceId));

        SimulatedFaultType scenario = request != null && request.getScenario() != null
                ? request.getScenario()
                : SimulatedFaultType.NORMAL;

        synchronized (st) {
            ApplianceNode node = st.node;
            if (node.getState() == ApplianceState.TRIPPED_LATCHED && scenario == SimulatedFaultType.NORMAL) {
                // Keep latched until physical/inspection reset, or allow explicit reset endpoint
                return node;
            }
            node.setActiveSimScenario(scenario);
            node.setUpdatedAt(Instant.now());
            if (scenario == SimulatedFaultType.NORMAL) {
                st.jitterIndex = 0.02;
                st.pinTempC = Math.min(st.pinTempC, 42.0);
                node.setFaultOrigin(FaultOrigin.NONE);
                node.setState(ApplianceState.ONLINE);
                node.setRelayClosed(true);
                node.setLastReason("Returned to normal operating profile");
            } else if (scenario == SimulatedFaultType.LOOSE_SOCKET_ARCING) {
                st.jitterIndex = 0.42;
                node.setLastReason("Injected loose-socket micro-arcing fault");
            } else if (scenario == SimulatedFaultType.THERMAL_PIN_DRIFT) {
                st.pinTempC = Math.max(st.pinTempC, 54.0);
                node.setLastReason("Injected pin thermal drift scenario");
            } else if (scenario == SimulatedFaultType.GRID_VOLTAGE_SURGE) {
                node.setLastReason("Injected upstream utility voltage surge");
            } else if (scenario == SimulatedFaultType.HIGH_TODAY_USAGE) {
                node.setLastReason("Injected high-today consumption surge");
            }
            try {
                applianceNodeRepository.save(node);
            } catch (Exception ignored) {
            }
            return node;
        }
    }

    public ResetResultDto performInspectionReset(String applianceId, String resolutionNotes) {
        ApplianceInternalState st = simulatorService.getInternalState(applianceId)
                .orElseThrow(() -> new NoSuchElementException("Appliance not found: " + applianceId));

        synchronized (st) {
            ApplianceNode node = st.node;
            ApplianceState prevState = node.getState();

            double postTripCurrent = (prevState == ApplianceState.FAULT_HARDWARE) ? 0.18 : 0.01;
            if (postTripCurrent > 0.1) {
                node.setState(ApplianceState.FAULT_HARDWARE);
                node.setRelayClosed(false);
                return new ResetResultDto(false, applianceId, prevState, ApplianceState.FAULT_HARDWARE,
                        false, postTripCurrent,
                        "Hardware fault: post-trip current > 0.1A detected (possible welded relay).",
                        Instant.now());
            }

            node.setActiveSimScenario(SimulatedFaultType.NORMAL);
            node.setState(ApplianceState.ONLINE);
            node.setRelayClosed(true);
            node.setFaultOrigin(FaultOrigin.NONE);
            node.setHealthScore(96);
            node.setLastReason("Inspection acknowledged & relay safely re-latched");
            node.setUpdatedAt(Instant.now());

            st.pinTempC = 34.5;
            st.jitterIndex = 0.02;
            st.learningTicksRemaining = 3;

            for (FaultEventLog logItem : st.faultLogs) {
                if (!logItem.isResolved()) {
                    logItem.setResolved(true);
                    logItem.setResolvedAt(Instant.now());
                    logItem.setResolutionNotes(resolutionNotes != null ? resolutionNotes : "Visual socket & plug pin inspection passed");
                }
            }

            try {
                applianceNodeRepository.save(node);
                List<FaultEventLog> unresolved = faultEventLogRepository.findByResolvedFalseOrderByTimestampDesc();
                for (FaultEventLog logItem : unresolved) {
                    if (applianceId.equals(logItem.getApplianceId())) {
                        logItem.setResolved(true);
                        logItem.setResolvedAt(Instant.now());
                        logItem.setResolutionNotes(resolutionNotes != null ? resolutionNotes : "Visual socket & plug pin inspection passed");
                        faultEventLogRepository.save(logItem);
                    }
                }
            } catch (Exception ignored) {
            }

            return new ResetResultDto(true, applianceId, prevState, ApplianceState.LEARNING,
                    true, postTripCurrent,
                    "Post-trip current verified (<0.1A). Relay closed and node calibrating.",
                    Instant.now());
        }
    }

    public void resetAllAppliances() {
        for (ApplianceNode node : simulatorService.getApplianceNodes()) {
            performInspectionReset(node.getId(), "Global operator reset");
        }
    }

    public List<FaultEventLog> getFaultLogs(String applianceId) {
        ApplianceInternalState st = simulatorService.getInternalState(applianceId)
                .orElseThrow(() -> new NoSuchElementException("Appliance not found: " + applianceId));
        try {
            List<FaultEventLog> dbLogs = faultEventLogRepository.findByApplianceIdOrderByTimestampDesc(
                    applianceId, PageRequest.of(0, 20));
            if (!dbLogs.isEmpty()) {
                return dbLogs;
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>(st.faultLogs);
    }
}
