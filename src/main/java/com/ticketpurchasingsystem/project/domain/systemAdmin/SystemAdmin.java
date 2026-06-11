package com.ticketpurchasingsystem.project.domain.systemAdmin;

import java.util.List;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;

public class SystemAdmin {

    private final String adminId;
    private final AdminPublisher adminPublisher;

    public SystemAdmin(String adminId, AdminPublisher adminPublisher) {
        this.adminId = adminId;
        this.adminPublisher = adminPublisher;
    }

    public List<ActiveOrderItem> getAllActiveOrders() {
        return adminPublisher.publishGetAllActiveOrders(this.adminId);
    }

    public List<HistoryOrderItem> getAllHistoryOrderItems() {
        return adminPublisher.publishGetAllOrdersHistory(this.adminId);
    }
}
