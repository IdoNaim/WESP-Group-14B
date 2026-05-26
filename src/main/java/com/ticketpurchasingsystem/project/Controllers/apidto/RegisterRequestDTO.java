package com.ticketpurchasingsystem.project.Controllers.apidto;

import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;

public class RegisterRequestDTO {

    private String userId;
    private String name;
    private String password;
    private String email;
    private UserGroupDiscount userGroupDiscount;

    public RegisterRequestDTO() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UserGroupDiscount getUserGroupDiscount() { return userGroupDiscount; }
    public void setUserGroupDiscount(UserGroupDiscount userGroupDiscount) { this.userGroupDiscount = userGroupDiscount; }
}
