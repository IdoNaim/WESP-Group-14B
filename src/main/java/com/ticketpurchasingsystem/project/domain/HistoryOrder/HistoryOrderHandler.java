package com.ticketpurchasingsystem.project.domain.HistoryOrder;

import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import java.util.HashMap;
import java.util.List;
import java.sql.Timestamp;

public class HistoryOrderHandler {

    public HistoryOrderHandler(IHistoryOrderRepo historyOrderRepo) {
        this.historyOrderRepo = historyOrderRepo;
    }

    public HistoryOrderItem saveHistoryOrder(String orderId, String userId, String eventId, int companyId, Timestamp purchaseDate, double price, List<String> seatIds, HashMap<String, Integer> standingAreaQuantities) {
        if(orderId == null || userId == null || eventId == null || seatIds == null || standingAreaQuantities == null) {
            return null;
        }
        HistoryOrderItem historyOrder = new HistoryOrderItem(orderId, userId, eventId, companyId, purchaseDate, price, seatIds, standingAreaQuantities);
        return historyOrder;
    }
}
