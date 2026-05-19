package com.ticketpurchasingsystem.project.domain.HistoryOrder;

import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;

public class HistoryOrderHandler {

    public HistoryOrderItem saveHistoryOrder(HistoryOrderDTO historyOrderDTO) {
        if(historyOrderDTO == null) {
            return null;
        }
        HistoryOrderItem historyOrder = new HistoryOrderItem(historyOrderDTO);
        return historyOrder;
    }
}
