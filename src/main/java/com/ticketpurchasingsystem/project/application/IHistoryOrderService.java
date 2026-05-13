package com.ticketpurchasingsystem.project.application;

import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import java.util.HashMap;
import java.util.List;
import java.sql.Timestamp;

public interface IHistoryOrderService {
    public boolean createHistoryOrder(String orderId, String userId, String eventId, int companyId, Timestamp purchaseDate, double price, List<String> seatIds, HashMap<String, Integer> standingAreaQuantities);
    public void getHistoryOrder(SessionToken sessionToken, String orderId);
    public void getAllHistoryOrdersByUser(SessionToken sessionToken, String userId);
    public void getAllHistoryOrdersByCompany(SessionToken sessionToken, int companyId);
    public void getAllHistoryOrders(SessionToken sessionToken);
}
