package com.ticketpurchasingsystem.project.domain.HistoryOrder;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
import java.util.Date;

public class HistoryOrderItem {
    private String id;
    private String name;
    private int price;
    private int quantity;
    private Date orderDate;
    public HistoryOrderItem(String id, String name, int price, int quantity, Date orderDate) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.orderDate = orderDate;
    }

    public HistoryOrderItem(ActiveOrderItem activeOrderItem, Date orderDate) {
        this.id = activeOrderItem.getId();
        this.name = activeOrderItem.getName();
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
