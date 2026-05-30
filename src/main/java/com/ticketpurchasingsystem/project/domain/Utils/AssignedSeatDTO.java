package com.ticketpurchasingsystem.project.domain.Utils;

public class AssignedSeatDTO {
    private String id;
    private boolean isBooked;
    private String orderId;
    private double price;
    public AssignedSeatDTO(String id, boolean isBooked, String orderId, double price) {
        this.id = id;
        this.isBooked = isBooked;
        this.orderId = orderId;
        this.price = price;
    }
    public String getId() {
        return id;
    }
    public boolean isBooked() {
        return isBooked;
    }
    public String getOrderId() {
        return orderId;
    }
    public double getPrice() {
        return price;
    }
    public void setId(String id) {
        this.id = id;
    }
    public void setBooked(boolean isBooked) {
        this.isBooked = isBooked;
    }
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    public void setPrice(double price) {
        this.price = price;
    }
}
