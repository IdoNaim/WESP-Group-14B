package com.ticketpurchasingsystem.project.domain.authentication;

public class NewSessionEvent {
<<<<<<< HEAD
    private final String userId;
=======

>>>>>>> origin/SESSION_TOKEN
    private final String sessionToken;

    public NewSessionEvent(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getSessionToken() {
        return sessionToken;
    }
}