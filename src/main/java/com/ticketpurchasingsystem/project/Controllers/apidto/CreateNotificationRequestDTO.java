package com.ticketpurchasingsystem.project.Controllers.apidto;

public class CreateNotificationRequestDTO {
    private String targetUserId;
    private String message;

    public CreateNotificationRequestDTO() {}

    public String getTargetUserId() { return targetUserId; }
    public String getMessage() { return message; }

    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }
    public void setMessage(String message) { this.message = message; }
}
