package com.ticketpurchasingsystem.project.domain.Utils;

import java.time.LocalDateTime;

public class NotificationDTO {
    private String id;
    private String userId;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;

    public NotificationDTO(String id, String userId, String message, boolean read, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
