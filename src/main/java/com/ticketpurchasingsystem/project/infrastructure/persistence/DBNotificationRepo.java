package com.ticketpurchasingsystem.project.infrastructure.persistence;

import com.ticketpurchasingsystem.project.domain.notification.INotificationRepo;
import com.ticketpurchasingsystem.project.domain.notification.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class DBNotificationRepo implements INotificationRepo {

    private final NotificationJpaRepository notificationJpaRepository;

    @Autowired
    public DBNotificationRepo(NotificationJpaRepository notificationJpaRepository) {
        this.notificationJpaRepository = notificationJpaRepository;
    }

    @Override
    public void save(Notification notification) {
        notificationJpaRepository.save(notification);
    }

    @Override
    public Optional<Notification> findById(String id) {
        return notificationJpaRepository.findById(id);
    }

    @Override
    public List<Notification> findByUserId(String userId) {
        return notificationJpaRepository.findByUserId(userId);
    }

    @Override
    public long countUnreadByUserId(String userId) {
        return notificationJpaRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    public void deleteAll() {
        notificationJpaRepository.deleteAll();
    }
}
