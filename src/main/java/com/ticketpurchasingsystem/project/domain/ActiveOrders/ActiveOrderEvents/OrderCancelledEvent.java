package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import org.springframework.context.ApplicationEvent;

public class OrderCancelledEvent extends ApplicationEvent {
    private final String userId;
    private final String orderId;

    public OrderCancelledEvent(Object source, String userId, String orderId) {
        super(source);
        this.userId = userId;
        this.orderId = orderId;
    }

    public String getUserId() { return userId; }
    public String getOrderId() { return orderId; }
}
