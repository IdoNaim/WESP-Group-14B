package com.ticketpurchasingsystem.project.domain.HistoryOrder;

import java.util.List;

public interface IHistoryOrderRepo {
    void save(HistoryOrderItem historyOrder);
    List<HistoryOrderItem> findAll();
    List<HistoryOrderItem> findByCompanyId(int companyId);
}
