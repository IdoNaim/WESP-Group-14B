package com.ticketpurchasingsystem.project.application;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.BarcodeDTO;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

public interface  IActiveOrderService {
    public void cancelActiveOrder(SessionToken sessionToken, String userId, String orderId);

    public ActiveOrderDTO getActiveOrderInfo(SessionToken sessionToken, String orderId) throws Exception ;
    public ActiveOrderItem createPendingOrder(SessionToken sessionToken, String userId, String eventId);
    public void addSeatsToActiveOrder(SessionToken sessionToken, String orderId, List<String> seatIds);
    public void addStandingAreaToActiveOrder(SessionToken sessionToken, String orderId, String areaId, int quantity);
    public void updateActiveOrder(SessionToken sessionToken, ActiveOrderDTO order);
    public boolean saveOrder(ActiveOrderItem order);
    public List<BarcodeDTO> completeOrder(IPaymentGateway paymentGateway, SessionToken sessionToken, PaymentDetails paymentDetails, String orderId, Integer age);
    public ActiveOrderDTO getActiveOrderByUserId(SessionToken sessionToken, String userId) throws Exception;
}