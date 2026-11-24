package com.gridmind.controller;

import com.gridmind.service.ApplianceManagementService;
import com.gridmind.service.BaselineProfileService;
import com.gridmind.service.VirtualApplianceSimulatorService;
import com.gridmind.websocket.TelemetryWebSocketHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    private final VirtualApplianceSimulatorService simulatorService;
    private final ApplianceManagementService managementService;
    private final BaselineProfileService baselineProfileService;
    private final TelemetryWebSocketHandler webSocketHandler;

    public SimulatorController(VirtualApplianceSimulatorService simulatorService,
                               ApplianceManagementService managementService,
                               BaselineProfileService baselineProfileService,
                               TelemetryWebSocketHandler webSocketHandler) {
        this.simulatorService = simulatorService;
        this.managementService = managementService;
        this.baselineProfileService = baselineProfileService;
        this.webSocketHandler = webSocketHandler;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("running", simulatorService.isRunning());
        status.put("intervalMs", simulatorService.getIntervalMs());
        status.put("kwhCostInr", simulatorService.getKwhCostInr());
        status.put("activeWebSocketClients", webSocketHandler.getActiveSessionCount());
        status.put("applianceCount", simulatorService.getApplianceNodes().size());
        status.put("trainedModel", baselineProfileService.getTrainedModelSummary());
        return ResponseEntity.ok(status);
    }

    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleSimulator(@RequestBody(required = false) Map<String, Boolean> body) {
        boolean next = (body != null && body.containsKey("running"))
                ? body.get("running")
                : !simulatorService.isRunning();
        simulatorService.setRunning(next);
        return getStatus();
    }

    @PostMapping("/reset-all")
    public ResponseEntity<Map<String, Object>> resetAll() {
        managementService.resetAllAppliances();
        return getStatus();
    }
}
