package com.ticketpurchasingsystem.project.domain.User.Events.UserEvents;

public class UserLogOutEvent extends UserEvents {
    private String userId;

    public UserLogOutEvent(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
