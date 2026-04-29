package com.ticketpurchasingsystem.project.domain.User;


public class UserDTO {
    private String userId;
    private String username;
    private String email;
    private boolean isLoggedIn;

    public UserDTO(String userId, String username, String email, boolean isLoggedIn) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.isLoggedIn = isLoggedIn;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }
}