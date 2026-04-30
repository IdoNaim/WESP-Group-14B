package com.ticketpurchasingsystem.project.domain.User.Events.UserEvents;

public class UserLogInEvent extends UserEvents {
    private String userId;
    private String sessionTokenStr;

    public UserLogInEvent(String userId, String sessionTokenStr) {
        this.userId = userId;
        this.sessionTokenStr = sessionTokenStr;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    public String getSessionToken() {
        return sessionTokenStr;
    }
}
