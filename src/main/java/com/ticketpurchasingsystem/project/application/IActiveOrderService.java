package com.ticketpurchasingsystem.project.application;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

public interface  IActiveOrderService {
    //public String createActiveOrder(String userId, String eventId, int quantity) ;
    public void cancelActiveOrder(String orderId) ;
    public void getActiveOrders(String userId) ;
    public void getActiveOrder(String orderId) ;
    public ActiveOrderItem createPendingOrder(SessionToken sessionToken, String userId, String eventId);
    public void addSeatsToActiveOrder(SessionToken sessionToken, String orderId, List<String> seatIds);
    public void updateActiveOrder(SessionToken sessionToken, ActiveOrderDTO order);
    public boolean saveOrder(ActiveOrderItem order);
    public List<BarcodeDTO> completeOrder(IPaymentGateway paymentGateway, SessionToken sessionToken, double amount, String orderId);
}