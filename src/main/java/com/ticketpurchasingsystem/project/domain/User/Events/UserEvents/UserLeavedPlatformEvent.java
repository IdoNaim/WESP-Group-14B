package com.ticketpurchasingsystem.project.domain.User.Events.UserEvents;

public class UserLeavedPlatformEvent {
    private String userId;
    private String sessionTokenStr;

    public UserLeavedPlatformEvent(String userId, String sessionTokenStr) {
        this.userId = userId;
        this.sessionTokenStr = sessionTokenStr;
    }

    public String getUserId() {
        return userId;
    }

    public String getSessionToken() {
        return sessionTokenStr;
    }
    
}
