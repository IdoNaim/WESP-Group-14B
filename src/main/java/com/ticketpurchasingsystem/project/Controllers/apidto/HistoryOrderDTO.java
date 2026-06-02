package com.ticketpurchasingsystem.project.Controllers.apidto;

public class HistoryOrderDTO {
    private String orderId;
    private String eventName;
    private String eventDate;
    private int quantity;
    private double totalPrice;

    public HistoryOrderDTO(String orderId, String eventName, String eventDate, int quantity, double totalPrice) {
        this.orderId = orderId;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
    
}
