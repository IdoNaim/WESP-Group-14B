package com.ticketpurchasingsystem.project.domain.ActiveOrders;

import java.sql.Timestamp;

public class ActiveOrderItem {
    private int orderId;
    private int userId;
    private int eventId;
    private int quantity;
    private String status;
    private Timestamp createdAt;


    public ActiveOrderItem(int orderId, int userId, int eventId, int quantity) {
        this.orderId = orderId;
        this.userId = userId;
        this.eventId = eventId;
        this.quantity = quantity;
        this.status = "active";
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getEventId() {
        return eventId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
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
