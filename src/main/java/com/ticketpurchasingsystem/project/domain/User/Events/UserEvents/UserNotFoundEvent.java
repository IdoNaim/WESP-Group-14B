package com.ticketpurchasingsystem.project.domain.User.Events.UserEvents;

public class UserNotFoundEvent extends UserEvents {
    private String userId;

    public UserNotFoundEvent(String userId) {
        this.userId = userId;
    }

    @Override
    public String getUserId() {
        return this.userId;
    }
    
}
