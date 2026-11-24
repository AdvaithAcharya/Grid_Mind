package com.gridmind.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "telemetry_samples")
@CompoundIndex(name = "appliance_time_idx", def = "{'applianceId': 1, 'timestamp': -1}")
public class TelemetrySample {

    @Id
    private String id;

    @Indexed
    private String applianceId;

    private ApplianceType applianceType;

    @Indexed
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

    public TelemetrySample() {}

    public TelemetrySample(String id, String applianceId, ApplianceType applianceType, Instant timestamp,
                           double voltageV, double currentA, double powerW, double powerFactor,
                           double pinTempC, double dTdtCPerMin, double jitterIndex,
                           double todayKwh, double expectedKwh, double p90Kwh, double estimatedCostInr,
                           boolean highTodayCaution, ApplianceState state, boolean relayClosed,
                           int healthScore, FaultOrigin faultOrigin) {
        this.id = id;
        this.applianceId = applianceId;
        this.applianceType = applianceType;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.voltageV = voltageV;
        this.currentA = currentA;
        this.powerW = powerW;
        this.powerFactor = powerFactor;
        this.pinTempC = pinTempC;
        this.dTdtCPerMin = dTdtCPerMin;
        this.jitterIndex = jitterIndex;
        this.todayKwh = todayKwh;
        this.expectedKwh = expectedKwh;
        this.p90Kwh = p90Kwh;
        this.estimatedCostInr = estimatedCostInr;
        this.highTodayCaution = highTodayCaution;
        this.state = state;
        this.relayClosed = relayClosed;
        this.healthScore = healthScore;
        this.faultOrigin = faultOrigin;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String applianceId;
        private ApplianceType applianceType;
        private Instant timestamp = Instant.now();
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

        public Builder id(String id) { this.id = id; return this; }
        public Builder applianceId(String applianceId) { this.applianceId = applianceId; return this; }
        public Builder applianceType(ApplianceType applianceType) { this.applianceType = applianceType; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder voltageV(double voltageV) { this.voltageV = voltageV; return this; }
        public Builder currentA(double currentA) { this.currentA = currentA; return this; }
        public Builder powerW(double powerW) { this.powerW = powerW; return this; }
        public Builder powerFactor(double powerFactor) { this.powerFactor = powerFactor; return this; }
        public Builder pinTempC(double pinTempC) { this.pinTempC = pinTempC; return this; }
        public Builder dTdtCPerMin(double dTdtCPerMin) { this.dTdtCPerMin = dTdtCPerMin; return this; }
        public Builder jitterIndex(double jitterIndex) { this.jitterIndex = jitterIndex; return this; }
        public Builder todayKwh(double todayKwh) { this.todayKwh = todayKwh; return this; }
        public Builder expectedKwh(double expectedKwh) { this.expectedKwh = expectedKwh; return this; }
        public Builder p90Kwh(double p90Kwh) { this.p90Kwh = p90Kwh; return this; }
        public Builder estimatedCostInr(double estimatedCostInr) { this.estimatedCostInr = estimatedCostInr; return this; }
        public Builder highTodayCaution(boolean highTodayCaution) { this.highTodayCaution = highTodayCaution; return this; }
        public Builder state(ApplianceState state) { this.state = state; return this; }
        public Builder relayClosed(boolean relayClosed) { this.relayClosed = relayClosed; return this; }
        public Builder healthScore(int healthScore) { this.healthScore = healthScore; return this; }
        public Builder faultOrigin(FaultOrigin faultOrigin) { this.faultOrigin = faultOrigin; return this; }

        public TelemetrySample build() {
            return new TelemetrySample(id, applianceId, applianceType, timestamp, voltageV, currentA,
                    powerW, powerFactor, pinTempC, dTdtCPerMin, jitterIndex, todayKwh, expectedKwh,
                    p90Kwh, estimatedCostInr, highTodayCaution, state, relayClosed, healthScore, faultOrigin);
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getApplianceId() { return applianceId; }
    public void setApplianceId(String applianceId) { this.applianceId = applianceId; }
    public ApplianceType getApplianceType() { return applianceType; }
    public void setApplianceType(ApplianceType applianceType) { this.applianceType = applianceType; }
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
}
