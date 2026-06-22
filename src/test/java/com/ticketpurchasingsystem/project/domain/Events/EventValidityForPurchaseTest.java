package com.ticketpurchasingsystem.project.domain.Events;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ticketpurchasingsystem.project.application.IEventService;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.IsValidEventIDEvent;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.event.Event;
import com.ticketpurchasingsystem.project.domain.event.EventAggregateListener;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;

/**
 * Verifies that a soft-cancelled event (isActive == false) is treated as invalid
 * for purchase, so order creation rejects it even though the row still exists.
 */
public class EventValidityForPurchaseTest {

    private IEventRepo mockRepo;
    private EventAggregateListener listener;

    private static final String EVENT_ID = "1";

    @BeforeEach
    void setUp() {
        mockRepo = mock(IEventRepo.class);
        IEventService mockEventService = mock(IEventService.class);
        IHistoryOrderRepo mockHistoryRepo = mock(IHistoryOrderRepo.class);
        listener = new EventAggregateListener(mockRepo, mockEventService, mockHistoryRepo);
    }

    @Test
    void GivenActiveEvent_WhenCheckValidity_ThenValid() {
        Event activeEvent = mock(Event.class);
        when(activeEvent.isActive()).thenReturn(true);
        when(mockRepo.findById(EVENT_ID)).thenReturn(activeEvent);

        IsValidEventIDEvent event = new IsValidEventIDEvent(this, EVENT_ID);
        listener.onIsValidEventIDEvent(event);

        assertTrue(event.isValid());
    }

    @Test
    void GivenCancelledEvent_WhenCheckValidity_ThenInvalid() {
        Event cancelledEvent = mock(Event.class);
        when(cancelledEvent.isActive()).thenReturn(false);
        when(mockRepo.findById(EVENT_ID)).thenReturn(cancelledEvent);

        IsValidEventIDEvent event = new IsValidEventIDEvent(this, EVENT_ID);
        listener.onIsValidEventIDEvent(event);

        assertFalse(event.isValid());
    }

    @Test
    void GivenMissingEvent_WhenCheckValidity_ThenInvalid() {
        when(mockRepo.findById(EVENT_ID)).thenReturn(null);

        IsValidEventIDEvent event = new IsValidEventIDEvent(this, EVENT_ID);
        listener.onIsValidEventIDEvent(event);

        assertFalse(event.isValid());
    }
}
