package com.ticketpurchasingsystem.project.domain.User.Events.UserEvents;

public class UserLogInEvent extends UserEvents {
    private String userId;
    private String password;

    public UserLogInEvent(String userId, String password) {
        this.userId = userId;
        this.password = password;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }
}
