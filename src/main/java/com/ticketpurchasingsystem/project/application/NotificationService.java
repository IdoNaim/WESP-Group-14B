package com.ticketpurchasingsystem.project.application;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Utils.NotificationDTO;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;
import com.ticketpurchasingsystem.project.domain.notification.INotificationRepo;
import com.ticketpurchasingsystem.project.domain.notification.NotificationHandler;

@Service
@Transactional(readOnly = true)
public class NotificationService implements INotificationService {

    private final NotificationHandler notificationHandler;

    @Autowired
    public NotificationService(INotificationRepo notificationRepo,
                               AuthenticationService authenticationService,
                               IHistoryOrderRepo historyOrderRepo,
                               IProdRepo prodRepo,
                               IEventRepo eventRepo) {
        this.notificationHandler = NotificationHandler.getInstance(notificationRepo, authenticationService,
                historyOrderRepo, prodRepo, eventRepo);
    }

    @Override
    @Transactional
    public NotificationDTO createNotification(String token, String targetUserId, String message) {
        return notificationHandler.createNotification(token, targetUserId, message);
    }

    @Override
    @Transactional
    public List<NotificationDTO> createNotificationsForEvent(String token, String eventId, String message) {
        return notificationHandler.createNotificationsForEvent(token, eventId, message);
    }

    @Override
    @Transactional
    public List<NotificationDTO> createNotificationsForProduction(String token, int companyId, String message) {
        return notificationHandler.createNotificationsForProduction(token, companyId, message);
    }

    @Override
    public List<NotificationDTO> getNotificationsForUser(String token) {
        return notificationHandler.getNotificationsForUser(token);
    }

    @Override
    public NotificationDTO getNotificationById(String token, String notificationId) {
        return notificationHandler.getNotificationById(token, notificationId);
    }

    @Override
    @Transactional
    public boolean markAsRead(String token, String notificationId) {
        return notificationHandler.markAsRead(token, notificationId);
    }

    @Override
    @Transactional
    public NotificationDTO createSystemNotification(String targetUserId, String message) {
        return notificationHandler.createSystemNotification(targetUserId, message);
    }

    @Override
    public long getUnreadCount(String token) {
        return notificationHandler.getUnreadCount(token);
    }
}
