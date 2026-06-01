package com.ticketpurchasingsystem.project.domain.notification;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

public class Notification {
    private final String id;
    private final String userId;
    private final String message;
    private final AtomicBoolean read = new AtomicBoolean(false);
    private final LocalDateTime createdAt;

    public Notification(String id, String userId, String message) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getMessage() { return message; }
    public boolean isRead() { return read.get(); }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean markAsRead() { return read.compareAndSet(false, true); }
}
