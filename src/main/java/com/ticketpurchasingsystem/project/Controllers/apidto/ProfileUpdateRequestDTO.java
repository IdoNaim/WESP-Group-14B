package com.ticketpurchasingsystem.project.Controllers.apidto;

import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;

public class ProfileUpdateRequestDTO {

    private String name;
    private String email;
    private UserGroupDiscount userGroupDiscount;

    public ProfileUpdateRequestDTO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UserGroupDiscount getUserGroupDiscount() { return userGroupDiscount; }
    public void setUserGroupDiscount(UserGroupDiscount userGroupDiscount) { this.userGroupDiscount = userGroupDiscount; }
}
