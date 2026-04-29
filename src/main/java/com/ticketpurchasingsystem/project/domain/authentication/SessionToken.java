package com.ticketpurchasingsystem.project.domain.authentication;

public class SessionToken {
    private String token;
    private String userId;
    private long expirationTime;

    public SessionToken(String token, String userId, long expirationTime) {
        this.token = token;
        this.userId = userId;
        this.expirationTime = expirationTime;
    }

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }

    public long getExpirationTime() {
        return expirationTime;
    }
}