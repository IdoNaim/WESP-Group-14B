package com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;

public class GetAllHistoryOrdersEvent extends GetAllEvent<HistoryOrderItem> {
    public GetAllHistoryOrdersEvent(String reqId) {
        super(reqId);
    }
}