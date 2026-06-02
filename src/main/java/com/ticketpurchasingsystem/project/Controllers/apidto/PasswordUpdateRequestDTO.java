package com.ticketpurchasingsystem.project.Controllers.apidto;

public class PasswordUpdateRequestDTO {
    private String currentPassword;
    private String newPassword;

    public PasswordUpdateRequestDTO() {}
    
    public String getCurrentPassword() {
        return currentPassword;
    }
    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
