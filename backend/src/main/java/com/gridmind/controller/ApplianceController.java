package com.gridmind.controller;

import com.gridmind.dto.ApplianceDetailDto;
import com.gridmind.dto.FaultSimRequest;
import com.gridmind.dto.ResetResultDto;
import com.gridmind.dto.TelemetryBroadcastDto;
import com.gridmind.model.ApplianceNode;
import com.gridmind.model.FaultEventLog;
import com.gridmind.service.ApplianceManagementService;
import com.gridmind.service.BaselineProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appliances")
public class ApplianceController {

    private final ApplianceManagementService managementService;
    private final BaselineProfileService baselineProfileService;

    public ApplianceController(ApplianceManagementService managementService,
                               BaselineProfileService baselineProfileService) {
        this.managementService = managementService;
        this.baselineProfileService = baselineProfileService;
    }

    @GetMapping
    public ResponseEntity<List<ApplianceNode>> listAppliances() {
        return ResponseEntity.ok(managementService.listAppliances());
    }

    @GetMapping("/telemetry/latest")
    public ResponseEntity<List<TelemetryBroadcastDto>> getLatestTelemetryAll() {
        return ResponseEntity.ok(managementService.getLatestTelemetryAll());
    }

    @GetMapping({"/{id}", "/{id}/telemetry"})
    public ResponseEntity<ApplianceDetailDto> getApplianceDetail(@PathVariable String id) {
        return ResponseEntity.ok(managementService.getApplianceDetail(id));
    }

    @GetMapping("/{id}/faults")
    public ResponseEntity<List<FaultEventLog>> getApplianceFaults(@PathVariable String id) {
        return ResponseEntity.ok(managementService.getFaultLogs(id));
    }

    @PostMapping({"/{id}/simulate", "/{id}/fault-sim"})
    public ResponseEntity<ApplianceNode> simulateFault(@PathVariable String id,
                                                       @RequestBody(required = false) FaultSimRequest request) {
        return ResponseEntity.ok(managementService.injectScenario(id, request));
    }

    @PostMapping("/{id}/reset")
    public ResponseEntity<ResetResultDto> resetAppliance(@PathVariable String id,
                                                         @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(managementService.performInspectionReset(id, notes));
    }

    @GetMapping("/baselines/summary")
    public ResponseEntity<Map<String, Object>> getBaselineSummary() {
        return ResponseEntity.ok(baselineProfileService.getTrainedModelSummary());
    }
}
