package com.ticketpurchasingsystem.project.domain.notification;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.INotificationService;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.CompletedOrderEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.OrderRefundedEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.SeatReleaseEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.StandingAreaReleaseEvent;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock private INotificationService notificationService;
    @Mock private AuthenticationService authenticationService;

    private NotificationEventListener listener;

    private static final String USER_ID   = "user-001";
    private static final String ORDER_ID  = "ORD-001";
    private static final String EVENT_ID  = "EVT-001";
    private static final String AREA_ID   = "GA";
    private static final String TOKEN     = "valid-token";
    private static final double AMOUNT    = 99.50;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(notificationService, authenticationService);
    }

    // ── CompletedOrderEvent ─────────────────────────────────────────────────

    @Test
    void GivenCompletedOrder_WhenOnOrderCompleted_ThenNotifyBuyer() {
        ActiveOrderDTO order = new ActiveOrderDTO(ORDER_ID, USER_ID, EVENT_ID,
                new Timestamp(System.currentTimeMillis()), List.of(), new HashMap<>());
        CompletedOrderEvent event = new CompletedOrderEvent(this, order, AMOUNT, 42);

        listener.onOrderCompleted(event);

        verify(notificationService).createSystemNotification(eq(USER_ID), contains(ORDER_ID));
    }

    @Test
    void GivenCompletedOrder_WhenOnOrderCompleted_ThenMessageContainsEventId() {
        ActiveOrderDTO order = new ActiveOrderDTO(ORDER_ID, USER_ID, EVENT_ID,
                new Timestamp(System.currentTimeMillis()), List.of(), new HashMap<>());
        CompletedOrderEvent event = new CompletedOrderEvent(this, order, AMOUNT, 42);

        listener.onOrderCompleted(event);

        verify(notificationService).createSystemNotification(eq(USER_ID), contains(EVENT_ID));
    }

    @Test
    void GivenCompletedOrder_WhenOnOrderCompleted_ThenMessageContainsAmount() {
        ActiveOrderDTO order = new ActiveOrderDTO(ORDER_ID, USER_ID, EVENT_ID,
                new Timestamp(System.currentTimeMillis()), List.of(), new HashMap<>());
        CompletedOrderEvent event = new CompletedOrderEvent(this, order, AMOUNT, 42);

        listener.onOrderCompleted(event);

        verify(notificationService).createSystemNotification(eq(USER_ID), contains("99.50"));
    }

    // ── SeatReleaseEvent ────────────────────────────────────────────────────

    @Test
    void GivenValidToken_WhenOnSeatsReleased_ThenNotifyUser() {
        when(authenticationService.validate(TOKEN)).thenReturn(true);
        when(authenticationService.getUser(TOKEN)).thenReturn(USER_ID);
        SeatReleaseEvent event = new SeatReleaseEvent(this, TOKEN, EVENT_ID,
                List.of("SEAT-1", "SEAT-2"), ORDER_ID);

        listener.onSeatsReleased(event);

        verify(notificationService).createSystemNotification(eq(USER_ID), contains("2"));
    }

    @Test
    void GivenValidToken_WhenOnSeatsReleased_ThenMessageContainsOrderId() {
        when(authenticationService.validate(TOKEN)).thenReturn(true);
        when(authenticationService.getUser(TOKEN)).thenReturn(USER_ID);
        SeatReleaseEvent event = new SeatReleaseEvent(this, TOKEN, EVENT_ID,
                List.of("SEAT-1"), ORDER_ID);

        listener.onSeatsReleased(event);

        verify(notificationService).createSystemNotification(eq(USER_ID), contains(ORDER_ID));
    }

    @Test
    void GivenInvalidToken_WhenOnSeatsReleased_ThenNoNotificationSent() {
        when(authenticationService.validate("bad-token")).thenReturn(false);
        SeatReleaseEvent event = new SeatReleaseEvent(this, "bad-token", EVENT_ID,
                List.of("SEAT-1"), ORDER_ID);

        listener.onSeatsReleased(event);

        verify(notificationService, never()).createSystemNotification(eq(USER_ID), contains(ORDER_ID));
    }

    @Test
    void GivenTokenThrows_WhenOnSeatsReleased_ThenNoNotificationSent() {
        when(authenticationService.validate(TOKEN)).thenThrow(new RuntimeException("auth down"));
        SeatReleaseEvent event = new SeatReleaseEvent(this, TOKEN, EVENT_ID,
                List.of("SEAT-1"), ORDER_ID);

        listener.onSeatsReleased(event);

        verify(notificationService, never()).createSystemNotification(eq(USER_ID), contains(ORDER_ID));
    }

    // ── StandingAreaReleaseEvent ────────────────────────────────────────────

    @Test
    void GivenValidToken_WhenOnStandingAreaReleased_ThenNotifyUser() {
        when(authenticationService.validate(TOKEN)).thenReturn(true);
        when(authenticationService.getUser(TOKEN)).thenReturn(USER_ID);
        StandingAreaReleaseEvent event = new StandingAreaReleaseEvent(this, TOKEN, EVENT_ID, AREA_ID, 3);

        listener.onStandingAreaReleased(event);

        verify(notificationService).createSystemNotification(eq(USER_ID), contains(AREA_ID));
    }

    @Test
    void GivenValidToken_WhenOnStandingAreaReleased_ThenMessageContainsQuantity() {
        when(authenticationService.validate(TOKEN)).thenReturn(true);
        when(authenticationService.getUser(TOKEN)).thenReturn(USER_ID);
        StandingAreaReleaseEvent event = new StandingAreaReleaseEvent(this, TOKEN, EVENT_ID, AREA_ID, 5);

        listener.onStandingAreaReleased(event);

        verify(notificationService).createSystemNotification(eq(USER_ID), contains("5"));
    }

    @Test
    void GivenInvalidToken_WhenOnStandingAreaReleased_ThenNoNotificationSent() {
        when(authenticationService.validate("bad-token")).thenReturn(false);
        StandingAreaReleaseEvent event = new StandingAreaReleaseEvent(this, "bad-token", EVENT_ID, AREA_ID, 2);

        listener.onStandingAreaReleased(event);

        verify(notificationService, never()).createSystemNotification(eq(USER_ID), contains(AREA_ID));
    }

    // ── OrderRefundedEvent ──────────────────────────────────────────────────

    @Test
    void GivenRefundEvent_WhenOnOrderRefunded_ThenNotifyUser() {
        OrderRefundedEvent event = new OrderRefundedEvent(this, USER_ID, ORDER_ID, AMOUNT);

        listener.onOrderRefunded(event);

        verify(notificationService).createSystemNotification(eq(USER_ID), contains(ORDER_ID));
    }

    @Test
    void GivenRefundEvent_WhenOnOrderRefunded_ThenMessageContainsAmount() {
        OrderRefundedEvent event = new OrderRefundedEvent(this, USER_ID, ORDER_ID, AMOUNT);

        listener.onOrderRefunded(event);

        verify(notificationService).createSystemNotification(eq(USER_ID), contains("99.50"));
    }

    @Test
    void GivenRefundEvent_WhenOnOrderRefunded_ThenCorrectUserNotified() {
        OrderRefundedEvent event = new OrderRefundedEvent(this, "other-user", ORDER_ID, 50.0);

        listener.onOrderRefunded(event);

        verify(notificationService).createSystemNotification(eq("other-user"), contains(ORDER_ID));
    }
}
