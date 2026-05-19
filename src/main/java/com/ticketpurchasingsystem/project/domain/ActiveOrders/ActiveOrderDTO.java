package com.ticketpurchasingsystem.project.domain.ActiveOrders;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ActiveOrderDTO {
    private String orderId;
    private String userId;
    private String eventId;
    private Timestamp createdAt;
    private List<String> seatIds;
    private HashMap<String, Integer> StandingAreaQuantities;

    public ActiveOrderDTO(String orderId, String userId, String eventId, Timestamp createdAt, List<String> seatIds, HashMap<String,Integer> standingAreaQuantities) {
        this.orderId = orderId;
        this.userId = userId;
        this.eventId = eventId;
        this.createdAt = createdAt;
        this.seatIds = seatIds;
        this.StandingAreaQuantities = standingAreaQuantities;
    }
    public ActiveOrderDTO(ActiveOrderItem other){
        this.orderId = other.getOrderId();
        this.userId = other.getUserId();
        this.eventId = other.getEventId();
        this.createdAt = new Timestamp(other.getCreatedAt().getTime());
        this.seatIds = new ArrayList<>(other.getSeatIds());
        this.StandingAreaQuantities = new HashMap<>(other.getStandingAreaQuantities());
    }
    public String getOrderId() {
        return orderId;
    }
    public String getUserId() {
        return userId;
    }
    public String getEventId() {
        return eventId;
    }
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public List<String> getSeatIds() {
        return seatIds;
    }
    public HashMap<String, Integer> getStandingAreaQuantities(){
        return StandingAreaQuantities;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    public void setSeatIds(List<String> seatIds){
        this.seatIds = seatIds;
    }
    public void setStandingAreaQuantities(HashMap<String, Integer> standingAreaQuantities){
        this.StandingAreaQuantities = standingAreaQuantities;
    }

}

