package com.ticketpurchasingsystem.project.domain.ActiveOrders;
import java.sql.Timestamp;

public class ActiveOrderDTO {
    private int orderId;
    private String userId;
    private int eventId;
    private int quantity;
    private String status;
    private Timestamp createdAt;

    public ActiveOrderDTO(int orderId, String userId, int eventId, int quantity, String status, Timestamp createdAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.eventId = eventId;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getOrderId() {
        return orderId;
    }
    public String getUserId() {
        return userId;
    }
    public int getEventId() {
        return eventId;
    }
    public int getQuantity() {
        return quantity;
    }
    public String getStatus() {
        return status;
    }
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public void setEventId(int eventId) {
        this.eventId = eventId;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

}

   