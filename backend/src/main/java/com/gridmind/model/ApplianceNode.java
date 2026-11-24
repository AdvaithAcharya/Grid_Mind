package com.gridmind.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "appliances")
public class ApplianceNode {

    @Id
    private String id;

    @Indexed(unique = true)
    private ApplianceType applianceType;

    private String displayName;
    private double ratedWatts;
    private ApplianceState state = ApplianceState.ONLINE;
    private boolean relayClosed = true;
    private int healthScore = 98;
    private FaultOrigin faultOrigin = FaultOrigin.NONE;
    private String lastReason;
    private SimulatedFaultType activeSimScenario = SimulatedFaultType.NORMAL;
    private Instant updatedAt = Instant.now();

    public ApplianceNode() {}

    public ApplianceNode(String id, ApplianceType applianceType, String displayName, double ratedWatts,
                         ApplianceState state, boolean relayClosed, int healthScore,
                         FaultOrigin faultOrigin, String lastReason,
                         SimulatedFaultType activeSimScenario, Instant updatedAt) {
        this.id = id;
        this.applianceType = applianceType;
        this.displayName = displayName;
        this.ratedWatts = ratedWatts;
        this.state = state != null ? state : ApplianceState.ONLINE;
        this.relayClosed = relayClosed;
        this.healthScore = healthScore;
        this.faultOrigin = faultOrigin != null ? faultOrigin : FaultOrigin.NONE;
        this.lastReason = lastReason;
        this.activeSimScenario = activeSimScenario != null ? activeSimScenario : SimulatedFaultType.NORMAL;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private ApplianceType applianceType;
        private String displayName;
        private double ratedWatts;
        private ApplianceState state = ApplianceState.ONLINE;
        private boolean relayClosed = true;
        private int healthScore = 98;
        private FaultOrigin faultOrigin = FaultOrigin.NONE;
        private String lastReason;
        private SimulatedFaultType activeSimScenario = SimulatedFaultType.NORMAL;
        private Instant updatedAt = Instant.now();

        public Builder id(String id) { this.id = id; return this; }
        public Builder applianceType(ApplianceType applianceType) { this.applianceType = applianceType; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder ratedWatts(double ratedWatts) { this.ratedWatts = ratedWatts; return this; }
        public Builder state(ApplianceState state) { this.state = state; return this; }
        public Builder relayClosed(boolean relayClosed) { this.relayClosed = relayClosed; return this; }
        public Builder healthScore(int healthScore) { this.healthScore = healthScore; return this; }
        public Builder faultOrigin(FaultOrigin faultOrigin) { this.faultOrigin = faultOrigin; return this; }
        public Builder lastReason(String lastReason) { this.lastReason = lastReason; return this; }
        public Builder activeSimScenario(SimulatedFaultType activeSimScenario) { this.activeSimScenario = activeSimScenario; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public ApplianceNode build() {
            return new ApplianceNode(id, applianceType, displayName, ratedWatts, state, relayClosed,
                    healthScore, faultOrigin, lastReason, activeSimScenario, updatedAt);
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public ApplianceType getApplianceType() { return applianceType; }
    public void setApplianceType(ApplianceType applianceType) { this.applianceType = applianceType; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public double getRatedWatts() { return ratedWatts; }
    public void setRatedWatts(double ratedWatts) { this.ratedWatts = ratedWatts; }
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
    public SimulatedFaultType getActiveSimScenario() { return activeSimScenario; }
    public void setActiveSimScenario(SimulatedFaultType activeSimScenario) { this.activeSimScenario = activeSimScenario; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
