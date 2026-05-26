package com.ticketpurchasingsystem.project.domain.HistoryOrder;

import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;

@Component
public class HistoryOrderHandler {

    public HistoryOrderItem saveHistoryOrder(HistoryOrderDTO historyOrderDTO) {
        if(historyOrderDTO == null) {
            return null;
        }
        HistoryOrderItem historyOrder = new HistoryOrderItem(historyOrderDTO);
        return historyOrder;
    }
}
