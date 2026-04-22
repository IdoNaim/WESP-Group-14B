package com.ticketpurchasingsystem.project.application;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
public interface  IActiveOrderService {
    public String createActiveOrder(int userId, int eventId, int quantity) ;
    public void cancelActiveOrder(int orderId) ;
    public void getActiveOrders(int userId) ;
    public void getActiveOrder(int orderId) ;
    public void completeActiveOrder(int orderId) ; // maybe add payment info as parameter?
    public void updateActiveOrder(int orderId, int quantity) ;
    public boolean saveOrder(ActiveOrderItem order);

    
}
