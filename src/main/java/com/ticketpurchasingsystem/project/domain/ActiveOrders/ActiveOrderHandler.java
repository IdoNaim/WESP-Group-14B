package com.ticketpurchasingsystem.project.domain.ActiveOrders;

import com.sun.java.accessibility.util.EventID;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

import java.util.List;
import java.util.Map;

public class ActiveOrderHandler {
    private loggerDef logger = loggerDef.getInstance();

    public ActiveOrderDTO getActiveOrderInfo(String userId, ActiveOrderItem order){
        if(userId == null || order == null || order.getUserId() == null ||order.getOrderId() == null|| !userId.equals(order.getUserId())){
            return null;
        }
        ActiveOrderDTO orderDTO = new ActiveOrderDTO(order);
        return orderDTO;
    }
    public boolean isUsersOrder(String userId, ActiveOrderItem order){
        return order.getUserId().equals(userId);
    }
    public boolean canReleaseSeats(List<String> seatsToRelease){
        return seatsToRelease != null && !seatsToRelease.isEmpty();
    }
    public boolean canReleaseStanding(Map<String, Integer> standingToRelease){
        return standingToRelease != null && !standingToRelease.isEmpty();
    }
    public boolean canCreateActiveOrder(ActiveOrderItem order){
        if(order == null){
            logger.error("Save order failed: Order is null");
            throw new IllegalArgumentException("Order cannot be null");
        }
        if(!isValidEventID(order.getEventId()) || !isValidOrderID(order.getOrderId())) {
            logger.error("Save order failed: Invalid order ID or event ID for order: " + order.getOrderId());
            throw new IllegalArgumentException("bad order ID or event ID");
        }
        return true;
    }
    public boolean isValidEventID(String eventID){

    }
    public boolean isValidOrderID(String orderId){

    }
}
