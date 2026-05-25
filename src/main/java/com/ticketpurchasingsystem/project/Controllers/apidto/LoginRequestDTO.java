package com.ticketpurchasingsystem.project.Controllers.apidto;

public class LoginRequestDTO {

    private String userId;
    private String password;

    public LoginRequestDTO() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
