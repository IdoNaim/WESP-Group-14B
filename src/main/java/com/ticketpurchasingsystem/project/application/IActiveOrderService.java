package com.ticketpurchasingsystem.project.application;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
public interface  IActiveOrderService {
    public String createActiveOrder(String userId, int eventId, int quantity) ;
    public void cancelActiveOrder(int orderId) ;
    public void getActiveOrders(String userId) ;
    public void getActiveOrder(int orderId) ;
    public void completeActiveOrder(int orderId) ; // maybe add payment info as parameter?
    public void updateActiveOrder(int orderId, int quantity) ;
    public boolean saveOrder(ActiveOrderItem order);
    public ActiveOrderDTO getOrderInfo(int orderId);

    
}
