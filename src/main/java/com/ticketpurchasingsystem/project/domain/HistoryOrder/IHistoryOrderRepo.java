package com.ticketpurchasingsystem.project.domain.HistoryOrder;

import java.util.List;

public interface IHistoryOrderRepo {
    void save(HistoryOrderItem historyOrder);
    List<HistoryOrderItem> findAll();
    List<HistoryOrderItem> findAllByCompanyId(int companyId);
    List<HistoryOrderItem> findAllByUserId(String userId);
}
