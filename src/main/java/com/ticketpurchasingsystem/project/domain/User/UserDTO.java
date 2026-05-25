package com.ticketpurchasingsystem.project.domain.User;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDTO {
    @JsonAlias({"id", "userId"})
    private String userId;

    @JsonAlias({"name", "username"})
    private String username;

    private String email;
    private UserGroupDiscount userGroupDiscount;
    
    public UserDTO() {
    }

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