package com.ticketpurchasingsystem.project.application;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Utils.NotificationDTO;
import com.ticketpurchasingsystem.project.domain.notification.INotificationRepo;
import com.ticketpurchasingsystem.project.domain.notification.Notification;

@Service
public class NotificationService implements INotificationService {

    private final INotificationRepo notificationRepo;
    private final AuthenticationService authenticationService;
    private final IHistoryOrderRepo historyOrderRepo;
    private final IProdRepo prodRepo;
    private final AtomicLong idCounter = new AtomicLong(0);

    public NotificationService(INotificationRepo notificationRepo,
                               AuthenticationService authenticationService,
                               IHistoryOrderRepo historyOrderRepo,
                               IProdRepo prodRepo) {
        this.notificationRepo = notificationRepo;
        this.authenticationService = authenticationService;
        this.historyOrderRepo = historyOrderRepo;
        this.prodRepo = prodRepo;
    }

    private String nextId() {
        return "NOTIF-" + idCounter.incrementAndGet();
    }

    private NotificationDTO toDTO(Notification n) {
        return new NotificationDTO(n.getId(), n.getUserId(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }

    private void requireValidToken(String token) {
        if (!authenticationService.validate(token)) {
            throw new UnauthorizedException("Invalid session token");
        }
    }

    private NotificationDTO saveFor(String userId, String message) {
        Notification notification = new Notification(nextId(), userId, message);
        notificationRepo.save(notification);
        return toDTO(notification);
    }

    @Override
    public NotificationDTO createNotification(String token, String targetUserId, String message) {
        requireValidToken(token);
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new IllegalArgumentException("Target user ID must not be empty");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message must not be empty");
        }
        return saveFor(targetUserId, message);
    }

    @Override
    public List<NotificationDTO> createNotificationsForEvent(String token, String eventId, String message) {
        requireValidToken(token);
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event ID must not be empty");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message must not be empty");
        }
        Set<String> userIds = historyOrderRepo.findAllByEventId(eventId).stream()
                .map(order -> order.getUserId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<NotificationDTO> created = new ArrayList<>();
        for (String userId : userIds) {
            created.add(saveFor(userId, message));
        }
        return created;
    }

    @Override
    public List<NotificationDTO> createNotificationsForProduction(String token, int companyId, String message) {
        requireValidToken(token);
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message must not be empty");
        }
        ProductionCompany company = prodRepo.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Production company not found: " + companyId));

        Set<String> userIds = new LinkedHashSet<>(company.getOwnerIds());
        userIds.addAll(company.getManagerTree().keySet());

        List<NotificationDTO> created = new ArrayList<>();
        for (String userId : userIds) {
            created.add(saveFor(userId, message));
        }
        return created;
    }

    @Override
    public List<NotificationDTO> getNotificationsForUser(String token) {
        requireValidToken(token);
        String userId = authenticationService.getUser(token);
        return notificationRepo.findByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationDTO getNotificationById(String token, String notificationId) {
        requireValidToken(token);
        Notification notification = notificationRepo.findById(notificationId);
        if (notification == null) {
            throw new NotFoundException("Notification not found");
        }
        String userId = authenticationService.getUser(token);
        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied: notification belongs to another user");
        }
        return toDTO(notification);
    }

    @Override
    public boolean markAsRead(String token, String notificationId) {
        requireValidToken(token);
        Notification notification = notificationRepo.findById(notificationId);
        if (notification == null) {
            throw new NotFoundException("Notification not found");
        }
        String userId = authenticationService.getUser(token);
        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied: notification belongs to another user");
        }
        notification.markAsRead();
        return true;
    }

    @Override
    public NotificationDTO createSystemNotification(String targetUserId, String message) {
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new IllegalArgumentException("Target user ID must not be empty");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message must not be empty");
        }
        return saveFor(targetUserId, message);
    }

    @Override
    public long getUnreadCount(String token) {
        requireValidToken(token);
        String userId = authenticationService.getUser(token);
        return notificationRepo.countUnreadByUserId(userId);
    }
}
