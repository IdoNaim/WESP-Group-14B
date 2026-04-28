package com.ticketpurchasingsystem.project.domain.authentication;

public class NewSessionEvent {

    private final String sessionToken;

    public NewSessionEvent(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getSessionToken() {
        return sessionToken;
    }
}