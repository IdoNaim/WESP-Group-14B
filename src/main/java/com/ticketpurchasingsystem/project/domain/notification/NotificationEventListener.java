package com.ticketpurchasingsystem.project.domain.notification;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.INotificationService;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.CompletedOrderEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.OrderCancelledEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.OrderRefundedEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.SeatReleaseEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.StandingAreaReleaseEvent;
import java.util.Objects;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.AppointmentRequestedEvent;
import com.ticketpurchasingsystem.project.domain.event.Events_Events.EventCancelledEvent;
import com.ticketpurchasingsystem.project.domain.event.Events_Events.EventUpdatedEvent;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

@Component
public class NotificationEventListener {

    private final INotificationService notificationService;
    private final AuthenticationService authenticationService;
    private final IHistoryOrderRepo historyOrderRepo;
    private final loggerDef logger = loggerDef.getInstance();

    public NotificationEventListener(INotificationService notificationService,
                                     AuthenticationService authenticationService,
                                     IHistoryOrderRepo historyOrderRepo) {
        this.notificationService = notificationService;
        this.authenticationService = authenticationService;
        this.historyOrderRepo = Objects.requireNonNull(historyOrderRepo, "historyOrderRepo must not be null");
    }

    @EventListener
    public void onOrderCompleted(CompletedOrderEvent event) {
        String userId = event.getOrder().getUserId();
        String orderId = event.getOrder().getOrderId();
        String eventId = event.getOrder().getEventId();
        String message = String.format(
                "Your order %s for event %s has been placed successfully. Total paid: %.2f",
                orderId, eventId, event.getAmountPaid());
        notificationService.createSystemNotification(userId, message);
    }

    @EventListener
    public void onOrderCancelled(OrderCancelledEvent event) {
        String message = String.format(
                "Your order %s has been cancelled.", event.getOrderId());
        notificationService.createSystemNotification(event.getUserId(), message);
    }

    @EventListener
    public void onSeatsReleased(SeatReleaseEvent event) {
        String userId = resolveUserId(event.getSessionToken());
        if (userId == null) return;
        int count = event.getSeatIds().size();
        String message = String.format(
                "Your %d seat(s) for order %s have been released.",
                count, event.getOrderID());
        notificationService.createSystemNotification(userId, message);
    }

    @EventListener
    public void onStandingAreaReleased(StandingAreaReleaseEvent event) {
        String userId = resolveUserId(event.getSessionToken());
        if (userId == null) return;
        String message = String.format(
                "Your %d ticket(s) in standing area %s for event %s have been released.",
                event.getQuantity(), event.getAreaID(), event.getEventID());
        notificationService.createSystemNotification(userId, message);
    }

    @EventListener
    public void onEventCancelled(EventCancelledEvent event) {
        Set<String> buyers = new LinkedHashSet<>();
        historyOrderRepo.findAllByEventId(event.getEventId())
                .forEach(order -> buyers.add(order.getUserId()));
        String message = String.format(
                "Event \"%s\" has been cancelled.", event.getEventName());
        for (String userId : buyers) {
            notificationService.createSystemNotification(userId, message);
        }
    }

    @EventListener
    public void onEventUpdated(EventUpdatedEvent event) {
        Set<String> buyers = new LinkedHashSet<>();
        historyOrderRepo.findAllByEventId(event.getEventId())
                .forEach(order -> buyers.add(order.getUserId()));
        String message = String.format(
                "Update to event \"%s\": %s", event.getEventName(), event.getChangeDescription());
        for (String userId : buyers) {
            notificationService.createSystemNotification(userId, message);
        }
    }

    // Published by activeOrderPublisher.publishRefund() — call that when a
    // completed order is refunded through IPaymentGateway.refund().
    @EventListener
    public void onOrderRefunded(OrderRefundedEvent event) {
        String message = String.format(
                "Your refund of %.2f for order %s has been processed.",
                event.getAmount(), event.getOrderId());
        notificationService.createSystemNotification(event.getUserId(), message);
    }

    @EventListener
    public void onAppointmentRequested(AppointmentRequestedEvent event) {
        String roleLabel = "OWNER".equals(event.getRole()) ? "an owner" : "a manager";
        String message = String.format(
                "%s invited you to join \"%s\" as %s. Accept or deny it in My Companies.",
                event.getAppointerId(), event.getCompanyName(), roleLabel);
        notificationService.createSystemNotification(event.getAppointeeId(), message);
    }

    private String resolveUserId(String sessionToken) {
        try {
            if (authenticationService.validate(sessionToken)) {
                return authenticationService.getUser(sessionToken);
            }
        } catch (Exception e) {
            logger.error("Failed to resolve userId for session token: " + e.getMessage());
        }
        return null;
    }
}
