package com.ticketpurchasingsystem.project.domain.Utils;

import java.util.HashMap;
import java.util.List;
import java.sql.Timestamp;

public class HistoryOrderDTO {
    public String orderId;
    public String userId;
    public String eventId;
    public Timestamp purchaseDate;
    public double price;
    public List<String> seatIds;
    public HashMap<String, Integer> StandingAreaQuantities;

    public HistoryOrderDTO(String orderId, String userId, String eventId, Timestamp purchaseDate, double price, List<String> seatIds, HashMap<String, Integer> standingAreaQuantities) {
        this.orderId = orderId;
        this.userId = userId;
        this.eventId = eventId;
        this.purchaseDate = purchaseDate;
        this.price = price;
        this.seatIds = seatIds;
        this.StandingAreaQuantities = standingAreaQuantities;
    }

    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public String getEventId() { return eventId; }
    public Timestamp getPurchaseDate() { return purchaseDate; }
    public double getPrice() { return price; }
    public List<String> getSeatIds() { return seatIds; }
    public HashMap<String, Integer> getStandingAreaQuantities() { return StandingAreaQuantities; }
}
