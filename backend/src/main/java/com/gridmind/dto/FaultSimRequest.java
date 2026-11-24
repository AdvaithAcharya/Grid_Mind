package com.gridmind.dto;

import com.gridmind.model.SimulatedFaultType;

public class FaultSimRequest {
    private SimulatedFaultType scenario = SimulatedFaultType.NORMAL;
    private String customNotes;

    public FaultSimRequest() {}

    public FaultSimRequest(SimulatedFaultType scenario, String customNotes) {
        this.scenario = scenario;
        this.customNotes = customNotes;
    }

    public SimulatedFaultType getScenario() { return scenario; }
    public void setScenario(SimulatedFaultType scenario) { this.scenario = scenario; }
    public String getCustomNotes() { return customNotes; }
    public void setCustomNotes(String customNotes) { this.customNotes = customNotes; }
}
