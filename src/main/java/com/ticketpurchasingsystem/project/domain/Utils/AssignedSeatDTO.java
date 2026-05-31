package com.ticketpurchasingsystem.project.domain.Utils;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AssignedSeatDTO {
    private String id;
    private boolean isBooked;
    private String orderId;
    private double priceForTicket;

    public AssignedSeatDTO(String id, boolean isBooked, String orderId, double priceForTicket) {
        this.id = id;
        this.isBooked = isBooked;
        this.orderId = orderId;
        this.priceForTicket = priceForTicket;
    }

    public String getId() {
        return id;
    }

    @JsonProperty("isBooked")
    public boolean isBooked() {
        return isBooked;
    }

    public String getOrderId() {
        return orderId;
    }

    public double getPriceForTicket() {
        return priceForTicket;
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

    public void setPriceForTicket(double priceForTicket) {
        this.priceForTicket = priceForTicket;
    }
}
