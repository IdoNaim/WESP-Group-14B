package com.ticketpurchasingsystem.project.domain.notification;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.ticketpurchasingsystem.project.domain.Utils.NotificationDTO;
import com.ticketpurchasingsystem.project.domain.exceptions.ForbiddenException;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @Column(name = "id", nullable = false, length = 255)
    private String id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Notification() {}

    public Notification(String id, String userId, String message) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("Target user ID must not be empty");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("Message must not be empty");
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public synchronized boolean markAsRead() {
        if (!read) {
            read = true;
            return true;
        }
        return false;
    }

    public void requireOwnedBy(String userId) {
        if (!this.userId.equals(userId)) {
            throw new ForbiddenException("Access denied: notification belongs to another user");
        }
    }

    public NotificationDTO toDTO() {
        return new NotificationDTO(id, userId, message, read, createdAt);
    }
}
