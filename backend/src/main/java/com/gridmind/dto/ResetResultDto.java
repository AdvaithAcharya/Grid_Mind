package com.gridmind.dto;

import com.gridmind.model.ApplianceState;

import java.time.Instant;

public class ResetResultDto {
    private boolean success;
    private String applianceId;
    private ApplianceState previousState;
    private ApplianceState currentState;
    private boolean relayClosed;
    private double verifiedPostTripCurrentA;
    private String message;
    private Instant timestamp;

    public ResetResultDto() {}

    public ResetResultDto(boolean success, String applianceId, ApplianceState previousState,
                          ApplianceState currentState, boolean relayClosed, double verifiedPostTripCurrentA,
                          String message, Instant timestamp) {
        this.success = success;
        this.applianceId = applianceId;
        this.previousState = previousState;
        this.currentState = currentState;
        this.relayClosed = relayClosed;
        this.verifiedPostTripCurrentA = verifiedPostTripCurrentA;
        this.message = message;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getApplianceId() { return applianceId; }
    public void setApplianceId(String applianceId) { this.applianceId = applianceId; }
    public ApplianceState getPreviousState() { return previousState; }
    public void setPreviousState(ApplianceState previousState) { this.previousState = previousState; }
    public ApplianceState getCurrentState() { return currentState; }
    public void setCurrentState(ApplianceState currentState) { this.currentState = currentState; }
    public boolean isRelayClosed() { return relayClosed; }
    public void setRelayClosed(boolean relayClosed) { this.relayClosed = relayClosed; }
    public double getVerifiedPostTripCurrentA() { return verifiedPostTripCurrentA; }
    public void setVerifiedPostTripCurrentA(double verifiedPostTripCurrentA) { this.verifiedPostTripCurrentA = verifiedPostTripCurrentA; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
