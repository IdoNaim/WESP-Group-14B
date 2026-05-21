package com.ticketpurchasingsystem.project.application;

import java.util.List;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;

public interface ISystemAdminService {
    List<ActiveOrderDTO> getAllActiveOrders(String token);
    List<HistoryOrderDTO> getAllHistoryOrders(String token);
}
