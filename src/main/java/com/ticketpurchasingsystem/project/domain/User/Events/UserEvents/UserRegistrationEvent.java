package com.ticketpurchasingsystem.project.domain.User.Events.UserEvents;

public class UserRegistrationEvent extends UserEvents {
    private String userId;

    public UserRegistrationEvent(String userId) {
        this.userId = userId;
    }

    @Override
    public String getUserId() {
        return userId;
    }
    
    
}
