package com.gridmind.dto;

import com.gridmind.model.ApplianceState;
import com.gridmind.model.ApplianceType;
import com.gridmind.model.FaultOrigin;
import com.gridmind.model.TelemetrySample;

import java.time.Instant;

public class TelemetryBroadcastDto {
    private String applianceId;
    private ApplianceType applianceType;
    private String displayName;
    private Instant timestamp;
    private double voltageV;
    private double currentA;
    private double powerW;
    private double powerFactor;
    private double pinTempC;
    private double dTdtCPerMin;
    private double jitterIndex;
    private double todayKwh;
    private double expectedKwh;
    private double p90Kwh;
    private double estimatedCostInr;
    private boolean highTodayCaution;
    private ApplianceState state;
    private boolean relayClosed;
    private int healthScore;
    private FaultOrigin faultOrigin;
    private String lastReason;

    public TelemetryBroadcastDto() {}

    public static TelemetryBroadcastDto from(TelemetrySample sample, String displayName, String lastReason) {
        TelemetryBroadcastDto dto = new TelemetryBroadcastDto();
        dto.setApplianceId(sample.getApplianceId());
        dto.setApplianceType(sample.getApplianceType());
        dto.setDisplayName(displayName);
        dto.setTimestamp(sample.getTimestamp());
        dto.setVoltageV(sample.getVoltageV());
        dto.setCurrentA(sample.getCurrentA());
        dto.setPowerW(sample.getPowerW());
        dto.setPowerFactor(sample.getPowerFactor());
        dto.setPinTempC(sample.getPinTempC());
        dto.setDTdtCPerMin(sample.getDTdtCPerMin());
        dto.setJitterIndex(sample.getJitterIndex());
        dto.setTodayKwh(sample.getTodayKwh());
        dto.setExpectedKwh(sample.getExpectedKwh());
        dto.setP90Kwh(sample.getP90Kwh());
        dto.setEstimatedCostInr(sample.getEstimatedCostInr());
        dto.setHighTodayCaution(sample.isHighTodayCaution());
        dto.setState(sample.getState());
        dto.setRelayClosed(sample.isRelayClosed());
        dto.setHealthScore(sample.getHealthScore());
        dto.setFaultOrigin(sample.getFaultOrigin());
        dto.setLastReason(lastReason);
        return dto;
    }

    // Getters and Setters
    public String getApplianceId() { return applianceId; }
    public void setApplianceId(String applianceId) { this.applianceId = applianceId; }
    public ApplianceType getApplianceType() { return applianceType; }
    public void setApplianceType(ApplianceType applianceType) { this.applianceType = applianceType; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public double getVoltageV() { return voltageV; }
    public void setVoltageV(double voltageV) { this.voltageV = voltageV; }
    public double getCurrentA() { return currentA; }
    public void setCurrentA(double currentA) { this.currentA = currentA; }
    public double getPowerW() { return powerW; }
    public void setPowerW(double powerW) { this.powerW = powerW; }
    public double getPowerFactor() { return powerFactor; }
    public void setPowerFactor(double powerFactor) { this.powerFactor = powerFactor; }
    public double getPinTempC() { return pinTempC; }
    public void setPinTempC(double pinTempC) { this.pinTempC = pinTempC; }
    public double getDTdtCPerMin() { return dTdtCPerMin; }
    public void setDTdtCPerMin(double dTdtCPerMin) { this.dTdtCPerMin = dTdtCPerMin; }
    public double getJitterIndex() { return jitterIndex; }
    public void setJitterIndex(double jitterIndex) { this.jitterIndex = jitterIndex; }
    public double getTodayKwh() { return todayKwh; }
    public void setTodayKwh(double todayKwh) { this.todayKwh = todayKwh; }
    public double getExpectedKwh() { return expectedKwh; }
    public void setExpectedKwh(double expectedKwh) { this.expectedKwh = expectedKwh; }
    public double getP90Kwh() { return p90Kwh; }
    public void setP90Kwh(double p90Kwh) { this.p90Kwh = p90Kwh; }
    public double getEstimatedCostInr() { return estimatedCostInr; }
    public void setEstimatedCostInr(double estimatedCostInr) { this.estimatedCostInr = estimatedCostInr; }
    public boolean isHighTodayCaution() { return highTodayCaution; }
    public void setHighTodayCaution(boolean highTodayCaution) { this.highTodayCaution = highTodayCaution; }
    public ApplianceState getState() { return state; }
    public void setState(ApplianceState state) { this.state = state; }
    public boolean isRelayClosed() { return relayClosed; }
    public void setRelayClosed(boolean relayClosed) { this.relayClosed = relayClosed; }
    public int getHealthScore() { return healthScore; }
    public void setHealthScore(int healthScore) { this.healthScore = healthScore; }
    public FaultOrigin getFaultOrigin() { return faultOrigin; }
    public void setFaultOrigin(FaultOrigin faultOrigin) { this.faultOrigin = faultOrigin; }
    public String getLastReason() { return lastReason; }
    public void setLastReason(String lastReason) { this.lastReason = lastReason; }
}
