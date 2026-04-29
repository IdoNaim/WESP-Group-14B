package com.ticketpurchasingsystem.project.domain.authentication;

public class NewSessionEvent {
    private final String userId;
    private final String sessionToken;

    public NewSessionEvent(String userId, String sessionToken) {
        this.userId = userId;
        this.sessionToken = sessionToken;
    }

    public String getUserId() {
        return userId;
    }

    public String getSessionToken() {
        return sessionToken;
    }
}