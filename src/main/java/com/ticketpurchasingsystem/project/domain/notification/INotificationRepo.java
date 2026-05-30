package com.ticketpurchasingsystem.project.domain.notification;

import java.util.List;

public interface INotificationRepo {
    void save(Notification notification);
    Notification findById(String id);
    List<Notification> findByUserId(String userId);
    long countUnreadByUserId(String userId);
}
