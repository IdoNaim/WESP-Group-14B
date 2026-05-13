package com.ticketpurchasingsystem.project.domain.HistoryOrder;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;

public class HistoryOrderItem {

    private String orderId;
    private String userId;
    private String eventId;
    private int companyId;
    private Timestamp purchaseDate;
    private double price;
    private List<String> seatIds;
    private HashMap<String, Integer> StandingAreaQuantities;

    public HistoryOrderItem() {}

    public HistoryOrderItem(String orderId, String userId, String eventId, int companyId, double price, List<String> seatIds, HashMap<String, Integer> standingAreaQuantities) {
        this.orderId = orderId;
        this.userId = userId;
        this.eventId = eventId;
        this.companyId = companyId;
        this.purchaseDate = new Timestamp(System.currentTimeMillis());
        this.price = price;
        this.seatIds = new ArrayList<>(seatIds);
        this.StandingAreaQuantities = new HashMap<>(standingAreaQuantities);
    }

    public HistoryOrderItem(HistoryOrderDTO dto) {
        this.orderId = dto.getOrderId();
        this.userId = dto.getUserId();
        this.eventId = dto.getEventId();
        this.companyId = dto.getCompanyId();
        this.purchaseDate = dto.getPurchaseDate();
        this.price = dto.getPrice();
        this.seatIds = new ArrayList<>(dto.getSeatIds());
        this.StandingAreaQuantities = new HashMap<>(dto.getStandingAreaQuantities());
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getCompanyId() { return companyId; }
    public void setCompanyId(int companyId) { this.companyId = companyId; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Timestamp getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(Timestamp purchaseDate) { this.purchaseDate = purchaseDate; }

    public HistoryOrderDTO makeDTO() {
        return new HistoryOrderDTO(orderId, userId, eventId, companyId, purchaseDate, price, seatIds, StandingAreaQuantities);
    }
}

