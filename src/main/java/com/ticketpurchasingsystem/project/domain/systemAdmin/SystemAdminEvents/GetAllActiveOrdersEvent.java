package com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;

public class GetAllActiveOrdersEvent extends GetAllEvent<ActiveOrderItem> {
    public GetAllActiveOrdersEvent(String reqId) {
        super(reqId);
    }
}