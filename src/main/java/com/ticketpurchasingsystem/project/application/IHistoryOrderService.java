package com.ticketpurchasingsystem.project.application;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

public interface IHistoryOrderService {
    public boolean createHistoryOrder(String orderId, String userId, String eventId, int companyId, Timestamp purchaseDate, double price, List<String> seatIds, HashMap<String, Integer> standingAreaQuantities, Integer transactionId, List<String> barcodes);
    default public boolean createHistoryOrder(String orderId, String userId, String eventId, int companyId, Timestamp purchaseDate, double price, List<String> seatIds, HashMap<String, Integer> standingAreaQuantities, Integer transactionId) {
        return createHistoryOrder(orderId, userId, eventId, companyId, purchaseDate, price, seatIds, standingAreaQuantities, transactionId, null);
    }
    default public boolean createHistoryOrder(String orderId, String userId, String eventId, int companyId, Timestamp purchaseDate, double price, List<String> seatIds, HashMap<String, Integer> standingAreaQuantities) {
        return createHistoryOrder(orderId, userId, eventId, companyId, purchaseDate, price, seatIds, standingAreaQuantities, null, null);
    }
    public HistoryOrderDTO getHistoryOrder(SessionToken sessionToken, String orderId);
    public List<HistoryOrderDTO> getAllHistoryOrdersByUser(SessionToken sessionToken,String userASk);
    public List<HistoryOrderDTO> getAllHistoryOrdersByCompany(SessionToken sessionToken, int companyId);
    public List<HistoryOrderDTO> getAllHistoryOrders(SessionToken sessionToken);
}
