package com.ticketpurchasingsystem.project.domain.ActiveOrders;

import java.util.List;
import java.util.Map;

public class ActiveOrderHandler {

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
}
