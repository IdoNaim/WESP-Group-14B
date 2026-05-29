package com.ticketpurchasingsystem.project.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.ticketpurchasingsystem.project.domain.notification.INotificationRepo;
import com.ticketpurchasingsystem.project.domain.notification.Notification;

@Repository
public class InMemoryNotificationRepo implements INotificationRepo {
    private final ConcurrentHashMap<String, Notification> store = new ConcurrentHashMap<>();

    @Override
    public void save(Notification notification) {
        store.put(notification.getId(), notification);
    }

    @Override
    public Notification findById(String id) {
        return store.get(id);
    }

    @Override
    public List<Notification> findByUserId(String userId) {
        List<Notification> result = new ArrayList<>();
        for (Notification n : store.values()) {
            if (n.getUserId().equals(userId)) {
                result.add(n);
            }
        }
        return result;
    }

    @Override
    public long countUnreadByUserId(String userId) {
        return store.values().stream()
                .filter(n -> n.getUserId().equals(userId) && !n.isRead())
                .count();
    }
}
