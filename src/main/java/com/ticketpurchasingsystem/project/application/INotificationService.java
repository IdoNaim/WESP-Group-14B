package com.ticketpurchasingsystem.project.application;

import java.util.List;

import com.ticketpurchasingsystem.project.domain.Utils.NotificationDTO;

public interface INotificationService {
    NotificationDTO createNotification(String token, String targetUserId, String message);
    List<NotificationDTO> getNotificationsForUser(String token);
    NotificationDTO getNotificationById(String token, String notificationId);
    boolean markAsRead(String token, String notificationId);
    long getUnreadCount(String token);
    List<NotificationDTO> createNotificationsForEvent(String token, String eventId, String message);
    List<NotificationDTO> createNotificationsForProduction(String token, int companyId, String message);
    NotificationDTO createSystemNotification(String targetUserId, String message);
}
