package com.ticketpurchasingsystem.project.application;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
public interface  IActiveOrderService {
    public String createActiveOrder(String userId, String eventId, int quantity) ;
    public void cancelActiveOrder(String orderId) ;
    public void getActiveOrders(String userId) ;
    public void getActiveOrder(String orderId) ;
    public void completeActiveOrder(String orderId) ; // maybe add payment info as parameter?
    public void updateActiveOrder(String orderId, int quantity) ;
    public boolean saveOrder(ActiveOrderItem order);
    public ActiveOrderDTO getActiveOrderInfo(String orderId);

    
}
