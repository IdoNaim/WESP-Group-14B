package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import org.springframework.context.ApplicationEvent;

public class OrderRefundedEvent extends ApplicationEvent {
    private final String userId;
    private final String orderId;
    private final double amount;

    public OrderRefundedEvent(Object source, String userId, String orderId, double amount) {
        super(source);
        this.userId = userId;
        this.orderId = orderId;
        this.amount = amount;
    }

    public String getUserId() { return userId; }
    public String getOrderId() { return orderId; }
    public double getAmount() { return amount; }
}
