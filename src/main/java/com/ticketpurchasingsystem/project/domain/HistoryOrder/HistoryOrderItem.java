package com.ticketpurchasingsystem.project.domain.HistoryOrder;

import java.sql.Timestamp;

public class HistoryOrderItem {
    private String orderId;
    private String userId;
    private String eventId;
    private int companyId;
    private int quantity;
    private Timestamp purchaseDate;

    public HistoryOrderItem() {}

    public HistoryOrderItem(String orderId, String userId, String eventId, int companyId, int quantity) {
        this.orderId = orderId;
        this.userId = userId;
        this.eventId = eventId;
        this.companyId = companyId;
        this.quantity = quantity;
        this.purchaseDate = new Timestamp(System.currentTimeMillis());
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public int getCompanyId() { return companyId; }
    public void setCompanyId(int companyId) { this.companyId = companyId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public Timestamp getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(Timestamp purchaseDate) { this.purchaseDate = purchaseDate; }
}
