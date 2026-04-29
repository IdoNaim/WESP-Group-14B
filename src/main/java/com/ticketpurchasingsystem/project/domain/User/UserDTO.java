package com.ticketpurchasingsystem.project.domain.User;

public class UserDTO {
    private String userId;
    private String username;
    private String email;
    private UserGroupDiscount userGroupDiscount;
    
    public UserDTO(String userId, String username, String email, UserGroupDiscount userGroupDiscount) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.userGroupDiscount = userGroupDiscount;
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

    public UserGroupDiscount getGroupDiscount() {
        return userGroupDiscount;
    }
}