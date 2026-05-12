package com.ticketpurchasingsystem.project.domain.ActiveOrders;

public class ActiveOrderHandler {

    public ActiveOrderDTO getActiveOrderInfo(String userId, ActiveOrderItem order){
        if(userId == null || order == null || order.getUserId() == null ||order.getOrderId() == null|| !userId.equals(order.getUserId())){
            return null;
        }
        ActiveOrderDTO orderDTO = new ActiveOrderDTO(order);
        return orderDTO;
    }
}
