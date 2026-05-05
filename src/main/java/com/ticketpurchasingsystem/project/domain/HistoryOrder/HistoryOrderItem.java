package com.ticketpurchasingsystem.project.domain.HistoryOrder;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
import java.util.Date;

public class HistoryOrderItem {
    private String id;
    private String userId;
    private int price;
    private int quantity;
    private Date orderDate;
    public HistoryOrderItem(String id, String userId, int price, int quantity, Date orderDate) {
        this.id = id;
        this.userId = userId;
        this.price = price;
        this.quantity = quantity;
        this.orderDate = orderDate;
    }

    public HistoryOrderItem(ActiveOrderItem activeOrderItem, Date orderDate) {
        this.id = activeOrderItem.getOrderId();
        this.userId = activeOrderItem.getUserId();
        this.price = activeOrderItem.getPrice();
        this.quantity = activeOrderItem.getQuantity();
        this.orderDate = orderDate;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public Date getOrderDate() {
        return orderDate;
    }
    
}
