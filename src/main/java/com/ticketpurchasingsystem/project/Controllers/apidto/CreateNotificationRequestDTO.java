package com.ticketpurchasingsystem.project.Controllers.apidto;

public class CreateNotificationRequestDTO {
    private String targetUserId;
    private String message;

    public String getTargetUserId() { return targetUserId; }
    public String getMessage() { return message; }
}
