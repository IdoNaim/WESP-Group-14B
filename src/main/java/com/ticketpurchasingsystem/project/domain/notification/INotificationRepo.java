package com.ticketpurchasingsystem.project.domain.notification;

import java.util.List;
import java.util.Optional;

public interface INotificationRepo {
    void save(Notification notification);
    Optional<Notification> findById(String id);
    List<Notification> findByUserId(String userId);
    long countUnreadByUserId(String userId);
    void deleteAll();
}
