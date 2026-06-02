package com.ticketpurchasingsystem.project.domain.notification;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ticketpurchasingsystem.project.domain.Utils.NotificationDTO;
import com.ticketpurchasingsystem.project.domain.exceptions.ForbiddenException;

public class Notification {
    private final String id;
    private final String userId;
    private final String message;
    private final AtomicBoolean read = new AtomicBoolean(false);
    private final LocalDateTime createdAt;

    public Notification(String id, String userId, String message) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("Target user ID must not be empty");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("Message must not be empty");
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

    public void requireOwnedBy(String userId) {
        if (!this.userId.equals(userId)) {
            throw new ForbiddenException("Access denied: notification belongs to another user");
        }
    }

    public NotificationDTO toDTO() {
        return new NotificationDTO(id, userId, message, read.get(), createdAt);
    }
}
