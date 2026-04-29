package com.ticketpurchasingsystem.project.domain.User.Events.UserEvents;

// in the future we can add more events like user registration, password change, etc. and we can also add more information to the events like timestamp, user id, etc.
//For now its just skeleton for the events and we can add more information to the events later on when we need it.
public abstract class UserEvents extends Events {
    private String userId;

    public abstract String getUserId();

}
