package com.ticketpurchasingsystem.project.application;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ticketpurchasingsystem.project.domain.Utils.NotificationDTO;
import com.ticketpurchasingsystem.project.domain.notification.INotificationRepo;
import com.ticketpurchasingsystem.project.domain.notification.Notification;

@Service
public class NotificationService implements INotificationService {

    private final INotificationRepo notificationRepo;
    private final AuthenticationService authenticationService;
    private long idCounter = 0;

    public NotificationService(INotificationRepo notificationRepo, AuthenticationService authenticationService) {
        this.notificationRepo = notificationRepo;
        this.authenticationService = authenticationService;
    }

    private synchronized String nextId() {
        return "NOTIF-" + (++idCounter);
    }

    private NotificationDTO toDTO(Notification n) {
        return new NotificationDTO(n.getId(), n.getUserId(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }

    @Override
    public NotificationDTO createNotification(String token, String targetUserId, String message) {
        if (!authenticationService.validate(token)) {
            throw new IllegalArgumentException("Invalid session token");
        }
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new IllegalArgumentException("Target user ID must not be empty");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message must not be empty");
        }
        Notification notification = new Notification(nextId(), targetUserId, message);
        notificationRepo.save(notification);
        return toDTO(notification);
    }

    @Override
    public List<NotificationDTO> getNotificationsForUser(String token) {
        if (!authenticationService.validate(token)) {
            throw new IllegalArgumentException("Invalid session token");
        }
        String userId = authenticationService.getUser(token);
        return notificationRepo.findByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationDTO getNotificationById(String token, String notificationId) {
        if (!authenticationService.validate(token)) {
            throw new IllegalArgumentException("Invalid session token");
        }
        Notification notification = notificationRepo.findById(notificationId);
        if (notification == null) {
            throw new IllegalArgumentException("Notification not found");
        }
        String userId = authenticationService.getUser(token);
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied: notification belongs to another user");
        }
        return toDTO(notification);
    }

    @Override
    public boolean markAsRead(String token, String notificationId) {
        if (!authenticationService.validate(token)) {
            throw new IllegalArgumentException("Invalid session token");
        }
        Notification notification = notificationRepo.findById(notificationId);
        if (notification == null) {
            throw new IllegalArgumentException("Notification not found");
        }
        String userId = authenticationService.getUser(token);
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied: notification belongs to another user");
        }
        notification.markAsRead();
        return true;
    }

    @Override
    public long getUnreadCount(String token) {
        if (!authenticationService.validate(token)) {
            throw new IllegalArgumentException("Invalid session token");
        }
        String userId = authenticationService.getUser(token);
        return notificationRepo.countUnreadByUserId(userId);
    }
}
