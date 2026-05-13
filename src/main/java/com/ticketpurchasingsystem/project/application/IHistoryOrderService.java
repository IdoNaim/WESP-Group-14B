package com.ticketpurchasingsystem.project.application;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

public interface IHistoryOrderService {
    public boolean createHistoryOrder(String orderId, String userId, String eventId, int companyId, Timestamp purchaseDate, double price, List<String> seatIds, HashMap<String, Integer> standingAreaQuantities);
    public HistoryOrderDTO getHistoryOrder(SessionToken sessionToken, String orderId);
    public List<HistoryOrderDTO> getAllHistoryOrdersByUser(SessionToken sessionToken, String userId);
    public List<HistoryOrderDTO> getAllHistoryOrdersByCompany(SessionToken sessionToken, int companyId);
    public List<HistoryOrderDTO> getAllHistoryOrders(SessionToken sessionToken);
}
