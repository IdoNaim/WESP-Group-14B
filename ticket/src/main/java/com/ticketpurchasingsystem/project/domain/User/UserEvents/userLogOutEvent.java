package com.ticketpurchasingsystem.project.domain.User.UserEvents;

public class userLogOutEvent extends userEvents {
    private String userName;

    public userLogOutEvent(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }
}
