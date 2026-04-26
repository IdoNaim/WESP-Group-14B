package com.ticketpurchasingsystem.project.domain.ActiveOrders;

import java.sql.Timestamp;

public class ActiveOrderItem {
    private String orderId;
    private String userId;
    private String eventId;
    private int quantity;
    private String status;
    private Timestamp createdAt;


    public ActiveOrderItem(String orderId, String userId, String eventId, int quantity) {
        this.orderId = orderId;
        this.userId = userId;
        this.eventId = eventId;
        this.quantity = quantity;
        this.status = "active";
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventId() {
        return eventId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if(quantity <= 0) {
            throw  new IllegalArgumentException("cant buy negative number of tickets");
        }
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public void editOrder(ActiveOrderItem order) {
        if(order.getOrderId() != this.orderId) {
            throw new IllegalArgumentException("Order ID cannot be changed");
        }
        this.quantity = order.getQuantity();
        this.status = order.getStatus();
    }
}
