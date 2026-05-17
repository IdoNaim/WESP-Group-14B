package com.ticketpurchasingsystem.project.application;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;

public interface ISystemAdminService {
    List<ActiveOrderItem> getAllActiveOrders();
    List<HistoryOrderItem> getAllHistoryOrders();
}
