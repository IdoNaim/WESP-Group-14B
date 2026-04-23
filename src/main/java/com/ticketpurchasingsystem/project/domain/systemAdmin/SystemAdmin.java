package com.ticketpurchasingsystem.project.domain.systemAdmin;

import java.util.List;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;

public class SystemAdmin {

    private final AdminInfo adminInfo;
    private final AdminPublisher adminPublisher;

    public SystemAdmin(AdminInfo adminInfo, AdminPublisher adminPublisher) {
        this.adminInfo = adminInfo;
        this.adminPublisher = adminPublisher;
    }

    public List<ActiveOrderItem> getAllActiveOrders() {
        return adminPublisher.publishGetAllActiveOrders(this.adminInfo.getId());
    }
    public List<HistoryOrderItem> getAllHistoryOrderItems() {
        return adminPublisher.publishGetAllOrdersHistory(this.adminInfo.getId());
    }
}
