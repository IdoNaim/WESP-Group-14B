package com.ticketpurchasingsystem.project.domain.authentication;

public class SessionToken {
    private String token;
    private long expirationTime;

    public SessionToken(String token, long expirationTime) {
        this.token = token;

        this.expirationTime = expirationTime;
    }

    public String getToken() {
        return token;
    }

    public long getExpirationTime() {
        return expirationTime;
    }
}