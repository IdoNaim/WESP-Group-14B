package com.ticketpurchasingsystem.project.application;

import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

public interface IHistoryOrderService {
    public void saveHistoryOrder(String orderId, String userId, int companyId, String eventName, int ticketsPurchased, String purchaseDate);
    public void deleteHistoryOrder(String orderId);
    public void getHistoryOrder(SessionToken sessionToken, String orderId);
    public void getAllHistoryOrdersByUser(SessionToken sessionToken, String userId);
    public void getAllHistoryOrdersByCompany(SessionToken sessionToken, int companyId);
    public void getAllHistoryOrders(SessionToken sessionToken);
}
