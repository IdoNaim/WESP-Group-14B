package com.ticketpurchasingsystem.project.domain.HistoryOrder;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "history_orders")
public class HistoryOrderItem {

    @Id
    @Column(name = "order_id", nullable = false, length = 255)
    private String orderId;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    @Column(name = "company_id", nullable = false)
    private int companyId;

    @Column(name = "purchase_date", nullable = false)
    private Timestamp purchaseDate;

    @Column(name = "price", nullable = false)
    private double price;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "history_order_seats", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "seat_id")
    private List<String> seatIds;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "history_order_standing_areas", joinColumns = @JoinColumn(name = "order_id"))
    @MapKeyColumn(name = "area_id")
    @Column(name = "quantity")
    private Map<String, Integer> StandingAreaQuantities;

    @Column(name = "transaction_id")
    private Integer transactionId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "history_order_barcodes", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "barcode")
    private List<String> barcodes;

    public HistoryOrderItem() {}

    public HistoryOrderItem(String orderId, String userId, String eventId, int companyId, double price, List<String> seatIds, Map<String, Integer> standingAreaQuantities, Integer transactionId) {
        this.orderId = orderId;
        this.userId = userId;
        this.eventId = eventId;
        this.companyId = companyId;
        this.purchaseDate = new Timestamp(System.currentTimeMillis());
        this.price = price;
        this.seatIds = new ArrayList<>(seatIds);
        this.StandingAreaQuantities = new HashMap<>(standingAreaQuantities);
        this.transactionId = transactionId;
        this.barcodes = new ArrayList<>();
    }

    public HistoryOrderItem(String orderId, String userId, String eventId, int companyId, double price, List<String> seatIds, Map<String, Integer> standingAreaQuantities) {
        this(orderId, userId, eventId, companyId, price, seatIds, standingAreaQuantities, null);
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
        this.transactionId = dto.getTransactionId();
        this.barcodes = dto.getBarcodes() != null ? new ArrayList<>(dto.getBarcodes()) : new ArrayList<>();
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

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public List<String> getSeatIds() { return seatIds; }
    public void setSeatIds(List<String> seatIds) { this.seatIds = seatIds; }

    public HashMap<String, Integer> getStandingAreaQuantities() {
        return StandingAreaQuantities != null ? new HashMap<>(StandingAreaQuantities) : new HashMap<>();
    }
    public void setStandingAreaQuantities(Map<String, Integer> standingAreaQuantities) {
        this.StandingAreaQuantities = standingAreaQuantities;
    }

    public Integer getTransactionId() { return transactionId; }
    public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }

    public List<String> getBarcodes() { return barcodes != null ? barcodes : new ArrayList<>(); }
    public void setBarcodes(List<String> barcodes) { this.barcodes = barcodes; }

    public HistoryOrderDTO makeDTO() {
        return new HistoryOrderDTO(orderId, userId, eventId, companyId, purchaseDate, price, seatIds, getStandingAreaQuantities(), transactionId, getBarcodes());
    }
}

