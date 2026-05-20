package com.ticketpurchasingsystem.project.domain.User;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// ignore extra UserInfo fields (password, sessionToken, etc.) when mapping to DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDTO {
    @JsonProperty("id")
    private String userId;
    @JsonProperty("name")
    private String username;
    private String email;
    @JsonProperty("userGroupDiscount")
    private UserGroupDiscount userGroupDiscount;

    public UserDTO() {}

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
