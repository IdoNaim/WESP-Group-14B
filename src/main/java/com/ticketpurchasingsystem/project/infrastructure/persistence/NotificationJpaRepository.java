package com.ticketpurchasingsystem.project.infrastructure.persistence;

import com.ticketpurchasingsystem.project.domain.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationJpaRepository extends JpaRepository<Notification, String> {
    List<Notification> findByUserId(String userId);

    long countByUserIdAndReadFalse(String userId);
}

