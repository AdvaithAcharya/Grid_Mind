package com.gridmind.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "baseline_profiles")
@CompoundIndex(name = "type_hour_weekend_idx", def = "{'applianceType': 1, 'hour': 1, 'isWeekend': 1}", unique = true)
public class BaselineProfileDoc {

    @Id
    private String id;

    private ApplianceType applianceType;
    private int hour; // 0 - 23
    private boolean isWeekend;
    private double expectedWatts;
    private double expectedP50Kwh;
    private double expectedP90Kwh;
    private double nominalPowerFactor;

    public BaselineProfileDoc() {}

    public BaselineProfileDoc(String id, ApplianceType applianceType, int hour, boolean isWeekend,
                              double expectedWatts, double expectedP50Kwh, double expectedP90Kwh,
                              double nominalPowerFactor) {
        this.id = id;
        this.applianceType = applianceType;
        this.hour = hour;
        this.isWeekend = isWeekend;
        this.expectedWatts = expectedWatts;
        this.expectedP50Kwh = expectedP50Kwh;
        this.expectedP90Kwh = expectedP90Kwh;
        this.nominalPowerFactor = nominalPowerFactor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private ApplianceType applianceType;
        private int hour;
        private boolean isWeekend;
        private double expectedWatts;
        private double expectedP50Kwh;
        private double expectedP90Kwh;
        private double nominalPowerFactor;

        public Builder id(String id) { this.id = id; return this; }
        public Builder applianceType(ApplianceType applianceType) { this.applianceType = applianceType; return this; }
        public Builder hour(int hour) { this.hour = hour; return this; }
        public Builder isWeekend(boolean isWeekend) { this.isWeekend = isWeekend; return this; }
        public Builder expectedWatts(double expectedWatts) { this.expectedWatts = expectedWatts; return this; }
        public Builder expectedP50Kwh(double expectedP50Kwh) { this.expectedP50Kwh = expectedP50Kwh; return this; }
        public Builder expectedP90Kwh(double expectedP90Kwh) { this.expectedP90Kwh = expectedP90Kwh; return this; }
        public Builder nominalPowerFactor(double nominalPowerFactor) { this.nominalPowerFactor = nominalPowerFactor; return this; }

        public BaselineProfileDoc build() {
            return new BaselineProfileDoc(id, applianceType, hour, isWeekend, expectedWatts,
                    expectedP50Kwh, expectedP90Kwh, nominalPowerFactor);
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public ApplianceType getApplianceType() { return applianceType; }
    public void setApplianceType(ApplianceType applianceType) { this.applianceType = applianceType; }
    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }
    public boolean isWeekend() { return isWeekend; }
    public void setWeekend(boolean weekend) { isWeekend = weekend; }
    public double getExpectedWatts() { return expectedWatts; }
    public void setExpectedWatts(double expectedWatts) { this.expectedWatts = expectedWatts; }
    public double getExpectedP50Kwh() { return expectedP50Kwh; }
    public void setExpectedP50Kwh(double expectedP50Kwh) { this.expectedP50Kwh = expectedP50Kwh; }
    public double getExpectedP90Kwh() { return expectedP90Kwh; }
    public void setExpectedP90Kwh(double expectedP90Kwh) { this.expectedP90Kwh = expectedP90Kwh; }
    public double getNominalPowerFactor() { return nominalPowerFactor; }
    public void setNominalPowerFactor(double nominalPowerFactor) { this.nominalPowerFactor = nominalPowerFactor; }
}
