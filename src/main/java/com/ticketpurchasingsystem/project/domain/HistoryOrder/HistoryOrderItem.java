package com.ticketpurchasingsystem.project.domain.HistoryOrder;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class HistoryOrderItem {
    private String id;
    private String userId;
    private String eventID;
    private double price;
    private HashMap<String, Integer> standingAreaQuantities;
    private List<String> seatIds;
    private Date orderDate;
    public HistoryOrderItem(String id, String userId, String eventID, double price, Date orderDate) {
        this.id = id;
        this.userId = userId;
        this.eventID = eventID;
        this.price = price;
        this.orderDate = orderDate;
        this.standingAreaQuantities = new HashMap<>();
        this.seatIds = new ArrayList<>();
    }
    public HistoryOrderItem(String id, String userId, String eventID, double price, Date orderDate, HashMap<String, Integer> standingAreaQuantities, List<String> seatIds) {
        this.id = id;
        this.userId = userId;
        this.eventID = eventID;
        this.price = price;
        this.orderDate = orderDate;
        this.standingAreaQuantities = standingAreaQuantities;
        this.seatIds = seatIds;
    }

    public HistoryOrderItem(ActiveOrderItem activeOrderItem,double price, Date orderDate) {
        this.id = activeOrderItem.getOrderId();
        this.userId = activeOrderItem.getUserId();
        this.eventID = activeOrderItem.getEventId();
        this.price = price;
        this.standingAreaQuantities = new HashMap<>(activeOrderItem.getStandingAreaQuantities());
        this.seatIds = new ArrayList<>(activeOrderItem.getSeatIds());
        this.orderDate = orderDate;
    }

    public String getId() {
        return id;
    }
    public double getPrice() {
        return price;
    }
    public Date getOrderDate() {
        return orderDate;
    }

    public String getEventID() {
        return eventID;
    }

    public List<String> getSeatIds() {
        return seatIds;
    }

    public String getUserId() {
        return userId;
    }

     public HashMap<String, Integer> getStandingAreaQuantities(){
        return standingAreaQuantities;
     }
}
