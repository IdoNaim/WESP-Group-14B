package com.ticketpurchasingsystem.project.application;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

public interface  IActiveOrderService {
    //public String createActiveOrder(String userId, String eventId, int quantity) ;
    public void cancelActiveOrder(String orderId) ;
    public void getActiveOrders(String userId) ;
    public void getActiveOrder(String orderId) ;
    public ActiveOrderItem createPendingOrder(SessionToken sessionToken, String userId, String eventId, int quantity);
    public void updateActiveOrder(SessionToken sessionToken, String orderId, int quantity) ;
    public boolean saveOrder(ActiveOrderItem order);
    public void completeOrder(IPaymentGateway paymentGateway, SessionToken sessionToken, double amount, String orderId);
    public boolean payment(IPaymentGateway paymentGateway, SessionToken sessionToken, double amount);
}