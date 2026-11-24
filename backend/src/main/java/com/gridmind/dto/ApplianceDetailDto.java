package com.gridmind.dto;

import com.gridmind.model.ApplianceNode;
import com.gridmind.model.BaselineProfileDoc;
import com.gridmind.model.TelemetrySample;

import java.util.List;

public class ApplianceDetailDto {
    private ApplianceNode appliance;
    private TelemetrySample latestTelemetry;
    private List<TelemetrySample> recentHistory;
    private List<BaselineProfileDoc> baselineProfile;

    public ApplianceDetailDto() {}

    public ApplianceDetailDto(ApplianceNode appliance, TelemetrySample latestTelemetry,
                              List<TelemetrySample> recentHistory, List<BaselineProfileDoc> baselineProfile) {
        this.appliance = appliance;
        this.latestTelemetry = latestTelemetry;
        this.recentHistory = recentHistory;
        this.baselineProfile = baselineProfile;
    }

    public ApplianceNode getAppliance() { return appliance; }
    public void setAppliance(ApplianceNode appliance) { this.appliance = appliance; }
    public TelemetrySample getLatestTelemetry() { return latestTelemetry; }
    public void setLatestTelemetry(TelemetrySample latestTelemetry) { this.latestTelemetry = latestTelemetry; }
    public List<TelemetrySample> getRecentHistory() { return recentHistory; }
    public void setRecentHistory(List<TelemetrySample> recentHistory) { this.recentHistory = recentHistory; }
    public List<BaselineProfileDoc> getBaselineProfile() { return baselineProfile; }
    public void setBaselineProfile(List<BaselineProfileDoc> baselineProfile) { this.baselineProfile = baselineProfile; }
}
