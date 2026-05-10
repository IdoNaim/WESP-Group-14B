package com.ticketpurchasingsystem.project.domain.ActiveOrders;

import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class ActiveOrderItem {
    private String orderId;
    private String userId;
    private String eventId;
    private Timestamp createdAt;
    private List<String> seatIds;
    private HashMap<String, Integer> StandingAreaQuantities;
    private boolean paid;
    

    public final static int EXPIRATION_TIME_MINUTES = 15;


    public ActiveOrderItem(String orderId, String userId, String eventId) {
        this.orderId = orderId;
        this.userId = userId;
        this.eventId = eventId;
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.seatIds = new ArrayList<>();
        this.StandingAreaQuantities = new HashMap<>();
        paid = false;
    }
    public ActiveOrderItem(ActiveOrderItem other) {
        this.orderId = other.getOrderId();
        this.userId = other.getUserId();
        this.eventId = other.getEventId();
        this.createdAt = new Timestamp(other.getCreatedAt().getTime());
        this.seatIds = new ArrayList<>(other.getSeatIds());
        this.StandingAreaQuantities = new HashMap<>(other.getStandingAreaQuantities());
        this.paid = other.isPaid();
    }

    public boolean isPaid() {
        return paid;
    }
    public void setPaid(boolean paid){
        this.paid = paid;
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

    public void addSeatIds(List<String> seatIds) {
        for (String id : seatIds) {
            this.seatIds.add(id);
        }
    }
    public void addStandingAreaQuantity(String areaId, int quantity) {
        this.StandingAreaQuantities.put(areaId, quantity);
    }

    public List<String> getSeatIds() {
        return seatIds;
    }
    public HashMap<String, Integer> getStandingAreaQuantities() {
        return StandingAreaQuantities;
    }

    public boolean isExpired() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        long minutesElapsed = (now.getTime() - createdAt.getTime()) / (60 * 1000);
        return minutesElapsed >= EXPIRATION_TIME_MINUTES;
    }


    // public int getQuantity() {
    //     return quantity;
    // }

    // public void setQuantity(int quantity) {
    //     if(quantity <= 0) {
    //         throw  new IllegalArgumentException("cant buy negative number of tickets");
    //     }
    //     this.quantity = quantity;
    // }

    public void editOrder(ActiveOrderItem order) {
        if (!order.getOrderId().equals(this.orderId)) {
            throw new IllegalArgumentException("Order ID cannot be changed");
        }
        this.seatIds = new ArrayList<>(order.getSeatIds());
        this.StandingAreaQuantities = new HashMap<>(order.getStandingAreaQuantities());
        setPaid(order.isPaid());
    }

    public void setSeatIds(List<String> seatIds) {
        this.seatIds = seatIds;
    }

    public void setStandingAreaQuantities(HashMap<String, Integer> standingAreaQuantities) {
        StandingAreaQuantities = standingAreaQuantities;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
