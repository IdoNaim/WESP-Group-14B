package com.ticketpurchasingsystem.project.domain.ActiveOrders;

import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "active_orders")
public class ActiveOrderItem {

    @Id
    @Column(name = "order_id", nullable = false, length = 255)
    private String orderId;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "active_order_seats", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "seat_id")
    private List<String> seatIds;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "active_order_standing_areas", joinColumns = @JoinColumn(name = "order_id"))
    @MapKeyColumn(name = "area_id")
    @Column(name = "quantity")
    private Map<String, Integer> StandingAreaQuantities;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "processing", nullable = false)
    private boolean processing;


    public final static int EXPIRATION_TIME_MINUTES = 15;

    protected ActiveOrderItem() {}

    public ActiveOrderItem(String orderId, String userId, String eventId) {
        this.orderId = orderId;
        this.userId = userId;
        this.eventId = eventId;
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.seatIds = new ArrayList<>();
        this.StandingAreaQuantities = new HashMap<>();
        this.processing = false;
    }
    public ActiveOrderItem(ActiveOrderItem other) {
        this.orderId = other.getOrderId();
        this.userId = other.getUserId();
        this.eventId = other.getEventId();
        this.createdAt = new Timestamp(other.getCreatedAt().getTime());
        this.seatIds = new ArrayList<>(other.getSeatIds());
        this.StandingAreaQuantities = new HashMap<>(other.getStandingAreaQuantities());
        this.version = other.version;
        this.processing = false;
    }
    public ActiveOrderItem(ActiveOrderDTO other){
        this.orderId = other.getOrderId();
        this.userId = other.getUserId();
        this.eventId = other.getEventId();
        this.createdAt = new Timestamp(other.getCreatedAt().getTime());
        this.seatIds = new ArrayList<>(other.getSeatIds());
        this.StandingAreaQuantities = new HashMap<>(other.getStandingAreaQuantities());
        this.processing = false;
    }

    public boolean markAsProcessing() {
        if (processing) return false;
        processing = true;
        return true;
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
    public void removeSeatIds(List<String> seatIds) {
        for (String id : seatIds) {
            this.seatIds.remove(id);
        }
    }
    public void addStandingAreaQuantity(String areaId, int quantity) {
        if(StandingAreaQuantities.containsKey(areaId)){
            int currQuantity = StandingAreaQuantities.get(areaId);
            StandingAreaQuantities.put(areaId, quantity + currQuantity);
        }
        else {
            this.StandingAreaQuantities.put(areaId, quantity);
        }
    }

    public List<String> getSeatIds() {
        return new ArrayList<>(seatIds);
    }
    public HashMap<String, Integer> getStandingAreaQuantities() {
        return new HashMap<>(StandingAreaQuantities);
    }

    public boolean isExpired() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        long minutesElapsed = (now.getTime() - createdAt.getTime()) / (60 * 1000);
        return minutesElapsed >= EXPIRATION_TIME_MINUTES;
    }

    public void editOrder(ActiveOrderItem order) {
        if (!order.getOrderId().equals(this.orderId)) {
            throw new IllegalArgumentException("Order ID cannot be changed");
        }
        this.seatIds = new ArrayList<>(order.getSeatIds());
        this.StandingAreaQuantities = new HashMap<>(order.getStandingAreaQuantities());
        //added this for tests:
        this.createdAt = new Timestamp(order.getCreatedAt().getTime());
    }

    public void setSeatIds(List<String> seatIds) {
        this.seatIds = seatIds;
    }

    public void setStandingAreaQuantities(Map<String, Integer> standingAreaQuantities) {
        StandingAreaQuantities = standingAreaQuantities;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    public boolean markAsNotProcessing() {
        if (!processing) return false;
        processing = false;
        return true;
    }
}