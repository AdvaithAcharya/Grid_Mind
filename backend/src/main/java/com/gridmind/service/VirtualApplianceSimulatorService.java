package com.gridmind.service;

import com.gridmind.dto.TelemetryBroadcastDto;
import com.gridmind.model.*;
import com.gridmind.repository.ApplianceNodeRepository;
import com.gridmind.repository.FaultEventLogRepository;
import com.gridmind.repository.TelemetrySampleRepository;
import com.gridmind.websocket.TelemetryWebSocketHandler;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class VirtualApplianceSimulatorService {

    private static final Logger log = LoggerFactory.getLogger(VirtualApplianceSimulatorService.class);

    private final ApplianceNodeRepository applianceNodeRepository;
    private final TelemetrySampleRepository telemetrySampleRepository;
    private final FaultEventLogRepository faultEventLogRepository;
    private final BaselineProfileService baselineProfileService;
    private final TelemetryWebSocketHandler telemetryWebSocketHandler;

    @Value("${gridmind.simulator.enabled:true}")
    private boolean simulatorEnabled;

    @Value("${gridmind.simulator.interval-ms:1000}")
    private long intervalMs;

    @Value("${gridmind.simulator.telemetry-history-limit:120}")
    private int historyLimit;

    @Value("${gridmind.simulator.kwh-cost-inr:8.25}")
    private double kwhCostInr;

    private final Map<String, ApplianceInternalState> internalStates = new ConcurrentHashMap<>();
    private final List<String> orderedIds = List.of(
            "app-fan-01",
            "app-ac-01",
            "app-fridge-01",
            "app-geyser-01",
            "app-kettle-01",
            "app-tv-01"
    );

    private volatile boolean running = true;
    private long globalTick = 0L;

    public VirtualApplianceSimulatorService(
            ApplianceNodeRepository applianceNodeRepository,
            TelemetrySampleRepository telemetrySampleRepository,
            FaultEventLogRepository faultEventLogRepository,
            BaselineProfileService baselineProfileService,
            TelemetryWebSocketHandler telemetryWebSocketHandler) {
        this.applianceNodeRepository = applianceNodeRepository;
        this.telemetrySampleRepository = telemetrySampleRepository;
        this.faultEventLogRepository = faultEventLogRepository;
        this.baselineProfileService = baselineProfileService;
        this.telemetryWebSocketHandler = telemetryWebSocketHandler;
    }

    @PostConstruct
    public void initAppliances() {
        this.running = simulatorEnabled;
        List<ApplianceNode> defaults = List.of(
                ApplianceNode.builder()
                        .id("app-fan-01").applianceType(ApplianceType.FAN)
                        .displayName("Table Fan").ratedWatts(75.0)
                        .state(ApplianceState.ONLINE).relayClosed(true).healthScore(98)
                        .faultOrigin(FaultOrigin.NONE).activeSimScenario(SimulatedFaultType.NORMAL)
                        .lastReason("Nominal inductive motor operation").build(),
                ApplianceNode.builder()
                        .id("app-ac-01").applianceType(ApplianceType.AC)
                        .displayName("Inverter AC").ratedWatts(1500.0)
                        .state(ApplianceState.ONLINE).relayClosed(true).healthScore(97)
                        .faultOrigin(FaultOrigin.NONE).activeSimScenario(SimulatedFaultType.NORMAL)
                        .lastReason("Dual-stage compressor steady").build(),
                ApplianceNode.builder()
                        .id("app-fridge-01").applianceType(ApplianceType.FRIDGE)
                        .displayName("Refrigerator").ratedWatts(180.0)
                        .state(ApplianceState.ONLINE).relayClosed(true).healthScore(99)
                        .faultOrigin(FaultOrigin.NONE).activeSimScenario(SimulatedFaultType.NORMAL)
                        .lastReason("Compressor duty cycle nominal").build(),
                ApplianceNode.builder()
                        .id("app-geyser-01").applianceType(ApplianceType.GEYSER)
                        .displayName("Storage Geyser").ratedWatts(2000.0)
                        .state(ApplianceState.ONLINE).relayClosed(true).healthScore(96)
                        .faultOrigin(FaultOrigin.NONE).activeSimScenario(SimulatedFaultType.NORMAL)
                        .lastReason("Resistive thermal load healthy").build(),
                ApplianceNode.builder()
                        .id("app-kettle-01").applianceType(ApplianceType.KETTLE)
                        .displayName("Electric Kettle").ratedWatts(1800.0)
                        .state(ApplianceState.ONLINE).relayClosed(true).healthScore(98)
                        .faultOrigin(FaultOrigin.NONE).activeSimScenario(SimulatedFaultType.NORMAL)
                        .lastReason("Element impedance within envelope").build(),
                ApplianceNode.builder()
                        .id("app-tv-01").applianceType(ApplianceType.TV)
                        .displayName("Smart OLED TV").ratedWatts(120.0)
                        .state(ApplianceState.ONLINE).relayClosed(true).healthScore(99)
                        .faultOrigin(FaultOrigin.NONE).activeSimScenario(SimulatedFaultType.NORMAL)
                        .lastReason("SMPS harmonic draw nominal").build()
        );

        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        for (ApplianceNode def : defaults) {
            ApplianceNode node = def;
            try {
                node = applianceNodeRepository.findById(def.getId()).orElseGet(() -> applianceNodeRepository.save(def));
            } catch (Exception e) {
                log.debug("Using in-memory seed for {}: {}", def.getId(), e.getMessage());
            }

            BaselineProfileDoc baseline = baselineProfileService.getCurrentBaseline(node.getApplianceType(), now);
            double initialKwh = Math.max(0.05, baseline.getExpectedP50Kwh() * 0.68);
            double initialPinTemp = 34.0 + (node.getRatedWatts() / 2000.0) * 8.0;

            ApplianceInternalState st = new ApplianceInternalState(node, initialPinTemp, initialKwh);
            internalStates.put(node.getId(), st);
        }

        // Generate one immediate baseline sample so REST consumers have data right away
        stepSimulation(false);
        log.info("Initialized {} virtual appliance nodes (interval={}ms).", internalStates.size(), intervalMs);
    }

    @Scheduled(fixedDelayString = "${gridmind.simulator.interval-ms:1000}")
    public void scheduledTick() {
        if (!running) {
            return;
        }
        stepSimulation(true);
    }

    private void stepSimulation(boolean broadcast) {
        globalTick++;
        ZonedDateTime nowZdt = ZonedDateTime.now(ZoneId.systemDefault());
        Instant now = Instant.now();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Shared grid base voltage (with slight realistic 230V drift)
        double gridBaseVoltage = 230.0 + Math.sin(globalTick * 0.12) * 1.8 + rng.nextDouble(-0.9, 0.9);

        for (String id : orderedIds) {
            ApplianceInternalState st = internalStates.get(id);
            if (st == null) continue;

            synchronized (st) {
                ApplianceNode node = st.node;
                BaselineProfileDoc baseline = baselineProfileService.getCurrentBaseline(node.getApplianceType(), nowZdt);

                double voltageV = gridBaseVoltage;
                double powerW;
                double pf = baseline.getNominalPowerFactor();
                double currentA;
                double jitter = rng.nextDouble(0.01, 0.05);
                double pinTemp = st.pinTempC;
                double dTdt;

                if (node.getState() == ApplianceState.TRIPPED_LATCHED || node.getState() == ApplianceState.FAULT_HARDWARE) {
                    node.setRelayClosed(false);
                    powerW = 0.0;
                    currentA = (node.getState() == ApplianceState.FAULT_HARDWARE) ? 0.18 : 0.0;
                    jitter = 0.0;
                    // Cool down gradually after relay opens
                    pinTemp = Math.max(29.0, pinTemp - 0.35);
                    dTdt = round2((pinTemp - st.pinTempC) * 60.0);
                    st.pinTempC = pinTemp;
                } else {
                    // Active closed relay simulation
                    node.setRelayClosed(true);

                    double nominalWatts = computeDutyCycleWatts(node.getApplianceType(), baseline.getExpectedWatts(), node.getRatedWatts(), globalTick);
                    SimulatedFaultType scenario = node.getActiveSimScenario();

                    switch (scenario) {
                        case LOOSE_SOCKET_ARCING -> {
                            jitter = Math.min(0.92, st.jitterIndex + rng.nextDouble(0.06, 0.14));
                            pf = Math.max(0.58, pf - rng.nextDouble(0.14, 0.24));
                            powerW = nominalWatts * rng.nextDouble(0.85, 1.18);
                            pinTemp = Math.min(82.0, pinTemp + rng.nextDouble(0.05, 0.12));
                        }
                        case THERMAL_PIN_DRIFT -> {
                            pinTemp = Math.min(84.0, pinTemp + rng.nextDouble(0.045, 0.075)); // ~2.7 - 4.5 C/min
                            pf = Math.max(0.72, pf - 0.06);
                            powerW = nominalWatts * rng.nextDouble(1.02, 1.10);
                            jitter = rng.nextDouble(0.05, 0.12);
                        }
                        case GRID_VOLTAGE_SURGE -> {
                            voltageV = 264.0 + rng.nextDouble(2.0, 14.0);
                            powerW = nominalWatts * (voltageV / 230.0);
                            pinTemp = Math.min(54.0, pinTemp + 0.015);
                            jitter = rng.nextDouble(0.04, 0.10);
                        }
                        case HIGH_TODAY_USAGE -> {
                            powerW = Math.max(nominalWatts * 1.48, node.getRatedWatts() * 0.92) + rng.nextDouble(-12.0, 15.0);
                            st.todayKwh += (powerW / 1000.0) * (1.0 / 36.0); // accelerated demo accumulation when testing high usage
                            pinTemp = Math.min(52.0, Math.max(38.0, pinTemp + rng.nextDouble(-0.02, 0.04)));
                            jitter = rng.nextDouble(0.02, 0.07);
                        }
                        case NORMAL -> {
                            double ambientTarget = 31.5 + (nominalWatts / 2000.0) * 11.5;
                            pinTemp = pinTemp + (ambientTarget - pinTemp) * 0.12 + rng.nextDouble(-0.08, 0.08);
                            powerW = nominalWatts * rng.nextDouble(0.95, 1.05);
                            jitter = rng.nextDouble(0.01, 0.06);
                        }
                        default -> {
                            powerW = nominalWatts;
                        }
                    }

                    powerW = Math.max(0.0, round2(powerW));
                    pf = Math.max(0.45, Math.min(1.0, round2(pf)));
                    voltageV = round2(voltageV);
                    currentA = (voltageV * pf) > 1.0 ? round2(powerW / (voltageV * pf)) : 0.0;
                    dTdt = round2((pinTemp - st.pinTempC) * 60.0);
                    st.pinTempC = pinTemp;
                    st.jitterIndex = round3(jitter);

                    // Accumulate kWh for normal ticks (1s real time)
                    if (scenario != SimulatedFaultType.HIGH_TODAY_USAGE) {
                        st.todayKwh += (powerW / 1000.0) / 3600.0;
                    }

                    evaluateFsm(node, st, voltageV, pinTemp, dTdt, jitter);
                }

                double todayKwh = round3(st.todayKwh);
                double expectedKwh = round3(baseline.getExpectedP50Kwh());
                double p90Kwh = round3(baseline.getExpectedP90Kwh());
                boolean highTodayCaution = todayKwh > p90Kwh || node.getActiveSimScenario() == SimulatedFaultType.HIGH_TODAY_USAGE;
                double costInr = round2(todayKwh * kwhCostInr);

                TelemetrySample sample = TelemetrySample.builder()
                        .applianceId(node.getId())
                        .applianceType(node.getApplianceType())
                        .timestamp(now)
                        .voltageV(round2(voltageV))
                        .currentA(round2(currentA))
                        .powerW(round2(powerW))
                        .powerFactor(round2(pf))
                        .pinTempC(round2(pinTemp))
                        .dTdtCPerMin(round2(dTdt))
                        .jitterIndex(round3(jitter))
                        .todayKwh(todayKwh)
                        .expectedKwh(expectedKwh)
                        .p90Kwh(p90Kwh)
                        .estimatedCostInr(costInr)
                        .highTodayCaution(highTodayCaution)
                        .state(node.getState())
                        .relayClosed(node.isRelayClosed())
                        .healthScore(node.getHealthScore())
                        .faultOrigin(node.getFaultOrigin())
                        .build();

                st.latestSample = sample;
                st.history.addLast(sample);
                while (st.history.size() > historyLimit) {
                    st.history.pollFirst();
                }

                if (broadcast) {
                    TelemetryBroadcastDto dto = TelemetryBroadcastDto.from(sample, node.getDisplayName(), node.getLastReason());
                    telemetryWebSocketHandler.broadcastTelemetry(dto);

                    // Persist sample periodically (every 5s) to MongoDB Atlas
                    if (globalTick % 5 == 0) {
                        persistSampleQuietly(sample, node);
                    }
                }
            }
        }
    }

    private void evaluateFsm(ApplianceNode node, ApplianceInternalState st,
                             double voltageV, double pinTemp, double dTdt, double jitter) {
        ApplianceState previous = node.getState();

        if (st.learningTicksRemaining > 0) {
            st.learningTicksRemaining--;
            node.setState(ApplianceState.LEARNING);
            node.setFaultOrigin(FaultOrigin.NONE);
            node.setHealthScore(95);
            node.setLastReason("Calibrating hourly p50/p90 baseline envelope");
            return;
        }

        if (jitter >= 0.72 || pinTemp >= 76.0) {
            node.setState(ApplianceState.TRIPPED_LATCHED);
            node.setRelayClosed(false);
            node.setFaultOrigin(FaultOrigin.APPLIANCE);
            node.setHealthScore(Math.max(12, 35 - (int) (jitter * 20)));
            String reason = jitter >= 0.72
                    ? String.format("Edge reflex <10ms trip: severe micro-arcing (jitter %.2f)", jitter)
                    : String.format("Edge reflex <10ms trip: critical pin thermal runaway (%.1f°C)", pinTemp);
            node.setLastReason(reason);
            node.setUpdatedAt(Instant.now());

            if (previous != ApplianceState.TRIPPED_LATCHED) {
                logFaultEvent(node, jitter >= 0.72 ? "ARC_TRIP" : "THERMAL_TRIP", FaultOrigin.APPLIANCE, reason);
                persistNodeQuietly(node);
            }
            return;
        }

        if (jitter > 0.35) {
            node.setState(ApplianceState.WARNING_ARCING);
            node.setFaultOrigin(FaultOrigin.APPLIANCE);
            node.setHealthScore(Math.max(38, (int) (82 - jitter * 45)));
            node.setLastReason(String.format("Loose-socket micro-arcing signature (jitter %.2f)", jitter));
            if (previous != ApplianceState.WARNING_ARCING) {
                logFaultEvent(node, "LOOSE_SOCKET_ARCING", FaultOrigin.APPLIANCE, node.getLastReason());
                persistNodeQuietly(node);
            }
            return;
        }

        if (dTdt >= 2.0 || pinTemp >= 55.0) {
            node.setState(ApplianceState.WARNING_DRIFT);
            node.setFaultOrigin(FaultOrigin.APPLIANCE);
            node.setHealthScore(Math.max(45, (int) (90 - Math.max(0, pinTemp - 45) * 1.4)));
            node.setLastReason(String.format("Pin thermal drift dT/dt=%.2f°C/min (T=%.1f°C)", dTdt, pinTemp));
            if (previous != ApplianceState.WARNING_DRIFT) {
                logFaultEvent(node, "THERMAL_PIN_DRIFT", FaultOrigin.APPLIANCE, node.getLastReason());
                persistNodeQuietly(node);
            }
            return;
        }

        if (voltageV >= 255.0) {
            node.setState(ApplianceState.WARNING_DRIFT);
            node.setFaultOrigin(FaultOrigin.GRID);
            node.setHealthScore(72);
            node.setLastReason(String.format("Upstream utility over-voltage surge (%.1fV) — Grid Fault", voltageV));
            if (previous == ApplianceState.ONLINE) {
                logFaultEvent(node, "GRID_VOLTAGE_SURGE", FaultOrigin.GRID, node.getLastReason());
                persistNodeQuietly(node);
            }
            return;
        }

        node.setState(ApplianceState.ONLINE);
        node.setRelayClosed(true);
        node.setFaultOrigin(FaultOrigin.NONE);
        if (node.getActiveSimScenario() == SimulatedFaultType.HIGH_TODAY_USAGE) {
            node.setHealthScore(84);
            node.setLastReason("Trending High Today — exceeding p90 hourly baseline");
        } else {
            node.setHealthScore(96 + ThreadLocalRandom.current().nextInt(0, 4));
            node.setLastReason("Safe operating envelope");
        }
    }

    private double computeDutyCycleWatts(ApplianceType type, double expectedW, double ratedW, long tick) {
        return switch (type) {
            case FAN -> expectedW + Math.sin(tick * 0.25) * 3.5;
            case AC -> {
                double wave = 0.72 + 0.28 * Math.sin(tick * 0.08);
                yield Math.min(ratedW, Math.max(280.0, expectedW * wave));
            }
            case FRIDGE -> {
                boolean compressorOn = (tick % 36) < 22;
                yield compressorOn ? Math.max(125.0, expectedW) : 14.0;
            }
            case GEYSER -> {
                boolean heating = (tick % 40) < 26;
                yield heating ? ratedW * 0.96 : 12.0;
            }
            case KETTLE -> {
                boolean boilPulse = (tick % 30) < 12;
                yield boilPulse ? ratedW * 0.98 : 4.5;
            }
            case TV -> expectedW + Math.sin(tick * 0.35) * 11.0;
        };
    }

    private void logFaultEvent(ApplianceNode node, String faultType, FaultOrigin origin, String msg) {
        FaultEventLog event = FaultEventLog.builder()
                .applianceId(node.getId())
                .applianceType(node.getApplianceType())
                .timestamp(Instant.now())
                .faultType(faultType)
                .faultOrigin(origin)
                .diagnosisMessage(msg)
                .resolved(false)
                .build();
        try {
            faultEventLogRepository.save(event);
        } catch (Exception e) {
            log.debug("In-memory fault log only (MongoDB unavailable): {}", e.getMessage());
        }
        ApplianceInternalState st = internalStates.get(node.getId());
        if (st != null) {
            st.faultLogs.addFirst(event);
            while (st.faultLogs.size() > 30) {
                st.faultLogs.removeLast();
            }
        }
    }

    private void persistSampleQuietly(TelemetrySample sample, ApplianceNode node) {
        try {
            telemetrySampleRepository.save(sample);
            applianceNodeRepository.save(node);
        } catch (Exception ignored) {
        }
    }

    private void persistNodeQuietly(ApplianceNode node) {
        try {
            applianceNodeRepository.save(node);
        } catch (Exception ignored) {
        }
    }

    public List<ApplianceNode> getApplianceNodes() {
        List<ApplianceNode> list = new ArrayList<>();
        for (String id : orderedIds) {
            ApplianceInternalState st = internalStates.get(id);
            if (st != null) {
                list.add(st.node);
            }
        }
        return list;
    }

    public Optional<ApplianceInternalState> getInternalState(String id) {
        return Optional.ofNullable(internalStates.get(id));
    }

    public List<TelemetryBroadcastDto> getLatestBroadcastSnapshot() {
        List<TelemetryBroadcastDto> list = new ArrayList<>();
        for (String id : orderedIds) {
            ApplianceInternalState st = internalStates.get(id);
            if (st != null && st.latestSample != null) {
                list.add(TelemetryBroadcastDto.from(st.latestSample, st.node.getDisplayName(), st.node.getLastReason()));
            }
        }
        return list;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public double getKwhCostInr() {
        return kwhCostInr;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    public static class ApplianceInternalState {
        public final ApplianceNode node;
        public volatile double pinTempC;
        public volatile double jitterIndex = 0.02;
        public volatile double todayKwh;
        public volatile int learningTicksRemaining = 0;
        public volatile TelemetrySample latestSample;
        public final Deque<TelemetrySample> history = new ConcurrentLinkedDeque<>();
        public final Deque<FaultEventLog> faultLogs = new ConcurrentLinkedDeque<>();

        public ApplianceInternalState(ApplianceNode node, double pinTempC, double todayKwh) {
            this.node = node;
            this.pinTempC = pinTempC;
            this.todayKwh = todayKwh;
        }
    }
}
