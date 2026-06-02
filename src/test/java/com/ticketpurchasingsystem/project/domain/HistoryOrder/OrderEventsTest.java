package com.ticketpurchasingsystem.project.domain.HistoryOrder;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.OrderEvents.OrderEvent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OrderEventsTest {

    private static class TestOrderEvent extends OrderEvent {
        public TestOrderEvent(String orderId, String eventType) {
            super(orderId, eventType);
        }
    }

    @Test
    public void testOrderEventGetters() {
        OrderEvent event = new TestOrderEvent("order-123", "ORDER_COMPLETED");
        assertEquals("order-123", event.getOrderId());
        assertEquals("ORDER_COMPLETED", event.getEventType());
    }
}
