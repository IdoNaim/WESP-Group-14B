
package com.ticketpurchasingsystem.project.domain.User.UserEvents;

public class userLogInEvent extends userEvents {
    private String userName;
    private String password;

    public userLogInEvent(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }
}
