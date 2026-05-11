package com.ticketpurchasingsystem.project.domain.Production.ProductionEvents;

public class IsUserRegisteredEvent {
    private final String userId;
    private boolean registered = false;

    public IsUserRegisteredEvent(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }
}
