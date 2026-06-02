package com.ticketpurchasingsystem.project.domain.notification;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Utils.NotificationDTO;
import com.ticketpurchasingsystem.project.domain.event.Event;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;
import com.ticketpurchasingsystem.project.domain.exceptions.ForbiddenException;
import com.ticketpurchasingsystem.project.domain.exceptions.NotFoundException;

public class NotificationHandler {

    private static volatile NotificationHandler instance;

    private INotificationRepo notificationRepo;
    private AuthenticationService authenticationService;
    private IHistoryOrderRepo historyOrderRepo;
    private IProdRepo prodRepo;
    private IEventRepo eventRepo;
    private final AtomicLong idCounter = new AtomicLong(0);

    private NotificationHandler(INotificationRepo notificationRepo,
                                AuthenticationService authenticationService,
                                IHistoryOrderRepo historyOrderRepo,
                                IProdRepo prodRepo,
                                IEventRepo eventRepo) {
        this.notificationRepo = notificationRepo;
        this.authenticationService = authenticationService;
        this.historyOrderRepo = historyOrderRepo;
        this.prodRepo = prodRepo;
        this.eventRepo = eventRepo;
    }

    public static NotificationHandler getInstance(INotificationRepo notificationRepo,
                                                  AuthenticationService authenticationService,
                                                  IHistoryOrderRepo historyOrderRepo,
                                                  IProdRepo prodRepo,
                                                  IEventRepo eventRepo) {
        if (instance == null) {
            synchronized (NotificationHandler.class) {
                if (instance == null) {
                    instance = new NotificationHandler(notificationRepo, authenticationService,
                            historyOrderRepo, prodRepo, eventRepo);
                }
            }
        }
        instance.notificationRepo = notificationRepo;
        instance.authenticationService = authenticationService;
        instance.historyOrderRepo = historyOrderRepo;
        instance.prodRepo = prodRepo;
        instance.eventRepo = eventRepo;
        return instance;
    }

    private String nextId() {
        return "NOTIF-" + idCounter.incrementAndGet();
    }

    private void requireValidToken(String token) {
        if (!authenticationService.validate(token)) {
            throw new com.ticketpurchasingsystem.project.application.UnauthorizedException("Invalid session token");
        }
    }

    private NotificationDTO saveFor(String userId, String message) {
        Notification notification = new Notification(nextId(), userId, message);
        notificationRepo.save(notification);
        return notification.toDTO();
    }

    public NotificationDTO createNotification(String token, String targetUserId, String message) {
        requireValidToken(token);
        if (!authenticationService.isAdmin(token)) {
            throw new ForbiddenException("Only administrators can send targeted notifications");
        }
        return saveFor(targetUserId, message);
    }

    public List<NotificationDTO> createNotificationsForEvent(String token, String eventId, String message) {
        requireValidToken(token);
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            throw new NotFoundException("Event not found: " + eventId);
        }
        ProductionCompany company = prodRepo.findById(event.getCompanyId())
                .orElseThrow(() -> new NotFoundException("Production company not found: " + event.getCompanyId()));
        if (!company.isOwnerOrManager(authenticationService.getUser(token))) {
            throw new ForbiddenException("Caller is not an owner or manager of this production company");
        }
        return historyOrderRepo.findAllByEventId(eventId).stream()
                .map(order -> order.getUserId())
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .map(userId -> saveFor(userId, message))
                .collect(Collectors.toList());
    }

    public List<NotificationDTO> createNotificationsForProduction(String token, int companyId, String message) {
        requireValidToken(token);
        ProductionCompany company = prodRepo.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Production company not found: " + companyId));
        if (!company.isOwnerOrManager(authenticationService.getUser(token))) {
            throw new ForbiddenException("Caller is not an owner or manager of this production company");
        }
        LinkedHashSet<String> userIds = new LinkedHashSet<>(company.getOwnerIds());
        userIds.addAll(company.getManagerTree().keySet());
        return userIds.stream()
                .map(userId -> saveFor(userId, message))
                .collect(Collectors.toList());
    }

    public List<NotificationDTO> getNotificationsForUser(String token) {
        requireValidToken(token);
        return notificationRepo.findByUserId(authenticationService.getUser(token)).stream()
                .map(Notification::toDTO)
                .collect(Collectors.toList());
    }

    public NotificationDTO getNotificationById(String token, String notificationId) {
        requireValidToken(token);
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        notification.requireOwnedBy(authenticationService.getUser(token));
        return notification.toDTO();
    }

    public boolean markAsRead(String token, String notificationId) {
        requireValidToken(token);
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        notification.requireOwnedBy(authenticationService.getUser(token));
        return notification.markAsRead();
    }

    public NotificationDTO createSystemNotification(String targetUserId, String message) {
        return saveFor(targetUserId, message);
    }

    public long getUnreadCount(String token) {
        requireValidToken(token);
        return notificationRepo.countUnreadByUserId(authenticationService.getUser(token));
    }
}
