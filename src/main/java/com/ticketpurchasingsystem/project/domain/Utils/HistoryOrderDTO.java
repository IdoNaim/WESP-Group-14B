package com.ticketpurchasingsystem.project.domain.Utils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HistoryOrderDTO {
    public String orderId;
    public String userId;
    public String eventId;
    public int companyId;
    public Timestamp purchaseDate;
    public double price;
    public List<String> seatIds;
    public HashMap<String, Integer> StandingAreaQuantities;
    public Integer transactionId;
    public List<String> barcodes;

    public HistoryOrderDTO(String orderId, String userId, String eventId, int companyId, Timestamp purchaseDate, double price, List<String> seatIds, HashMap<String, Integer> standingAreaQuantities, Integer transactionId, List<String> barcodes) {
        this.orderId = orderId;
        this.userId = userId;
        this.eventId = eventId;
        this.companyId = companyId;
        this.purchaseDate = purchaseDate;
        this.price = price;
        this.seatIds = seatIds;
        this.StandingAreaQuantities = standingAreaQuantities;
        this.transactionId = transactionId;
        this.barcodes = barcodes != null ? new ArrayList<>(barcodes) : new ArrayList<>();
    }

    public HistoryOrderDTO(String orderId, String userId, String eventId, int companyId, Timestamp purchaseDate, double price, List<String> seatIds, HashMap<String, Integer> standingAreaQuantities, Integer transactionId) {
        this(orderId, userId, eventId, companyId, purchaseDate, price, seatIds, standingAreaQuantities, transactionId, null);
    }

    public HistoryOrderDTO(String orderId, String userId, String eventId, int companyId, Timestamp purchaseDate, double price, List<String> seatIds, HashMap<String, Integer> standingAreaQuantities) {
        this(orderId, userId, eventId, companyId, purchaseDate, price, seatIds, standingAreaQuantities, null, null);
    }

    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public String getEventId() { return eventId; }
    public int getCompanyId() { return companyId; }
    public Timestamp getPurchaseDate() { return purchaseDate; }
    public double getPrice() { return price; }
    public List<String> getSeatIds() { return seatIds; }
    public HashMap<String, Integer> getStandingAreaQuantities() { return StandingAreaQuantities; }
    public Integer getTransactionId() { return transactionId; }
    public List<String> getBarcodes() { return barcodes; }
}
