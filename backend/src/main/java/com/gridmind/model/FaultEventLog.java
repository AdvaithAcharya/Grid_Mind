package com.gridmind.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "fault_event_logs")
public class FaultEventLog {

    @Id
    private String id;

    @Indexed
    private String applianceId;

    private ApplianceType applianceType;

    @Indexed
    private Instant timestamp;

    private String faultType;
    private FaultOrigin faultOrigin;
    private String diagnosisMessage;
    private boolean resolved = false;
    private Instant resolvedAt;
    private String resolutionNotes;

    public FaultEventLog() {}

    public FaultEventLog(String id, String applianceId, ApplianceType applianceType, Instant timestamp,
                         String faultType, FaultOrigin faultOrigin, String diagnosisMessage,
                         boolean resolved, Instant resolvedAt, String resolutionNotes) {
        this.id = id;
        this.applianceId = applianceId;
        this.applianceType = applianceType;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.faultType = faultType;
        this.faultOrigin = faultOrigin;
        this.diagnosisMessage = diagnosisMessage;
        this.resolved = resolved;
        this.resolvedAt = resolvedAt;
        this.resolutionNotes = resolutionNotes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String applianceId;
        private ApplianceType applianceType;
        private Instant timestamp = Instant.now();
        private String faultType;
        private FaultOrigin faultOrigin;
        private String diagnosisMessage;
        private boolean resolved = false;
        private Instant resolvedAt;
        private String resolutionNotes;

        public Builder id(String id) { this.id = id; return this; }
        public Builder applianceId(String applianceId) { this.applianceId = applianceId; return this; }
        public Builder applianceType(ApplianceType applianceType) { this.applianceType = applianceType; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder faultType(String faultType) { this.faultType = faultType; return this; }
        public Builder faultOrigin(FaultOrigin faultOrigin) { this.faultOrigin = faultOrigin; return this; }
        public Builder diagnosisMessage(String diagnosisMessage) { this.diagnosisMessage = diagnosisMessage; return this; }
        public Builder resolved(boolean resolved) { this.resolved = resolved; return this; }
        public Builder resolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; return this; }
        public Builder resolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; return this; }

        public FaultEventLog build() {
            return new FaultEventLog(id, applianceId, applianceType, timestamp, faultType,
                    faultOrigin, diagnosisMessage, resolved, resolvedAt, resolutionNotes);
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
    public String getFaultType() { return faultType; }
    public void setFaultType(String faultType) { this.faultType = faultType; }
    public FaultOrigin getFaultOrigin() { return faultOrigin; }
    public void setFaultOrigin(FaultOrigin faultOrigin) { this.faultOrigin = faultOrigin; }
    public String getDiagnosisMessage() { return diagnosisMessage; }
    public void setDiagnosisMessage(String diagnosisMessage) { this.diagnosisMessage = diagnosisMessage; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
}
