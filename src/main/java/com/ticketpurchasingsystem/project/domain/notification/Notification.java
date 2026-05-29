package com.ticketpurchasingsystem.project.domain.notification;

import java.time.LocalDateTime;

public class Notification {
    private final String id;
    private final String userId;
    private final String message;
    private boolean read;
    private final LocalDateTime createdAt;

    public Notification(String id, String userId, String message) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void markAsRead() { this.read = true; }
}
