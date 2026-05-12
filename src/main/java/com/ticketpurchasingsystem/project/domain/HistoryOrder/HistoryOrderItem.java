package com.ticketpurchasingsystem.project.domain.HistoryOrder;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HistoryOrderItem {

    private String orderId;
    private String userId;
    private String eventId;
    private Timestamp purchaseDate;
    private double price;
    private List<String> seatIds;
    private HashMap<String, Integer> StandingAreaQuantities;

    public HistoryOrderItem() {}

    public HistoryOrderItem(String orderId, String userId, String eventId, double price, List<String> seatIds, HashMap<String, Integer> standingAreaQuantities) {
        this.orderId = orderId;
        this.userId = userId;
        this.eventId = eventId;
        this.purchaseDate = new Timestamp(System.currentTimeMillis());
        this.price = price;
        this.seatIds = new ArrayList<>(seatIds);
        this.StandingAreaQuantities = new HashMap<>(standingAreaQuantities);
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Timestamp getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(Timestamp purchaseDate) { this.purchaseDate = purchaseDate; }

    public HistoryOrderDTO makeDTO() {
        return new HistoryOrderDTO(orderId, userId, eventId, purchaseDate, price, seatIds, StandingAreaQuantities);
    }
}

