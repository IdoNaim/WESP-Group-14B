package com.ticketpurchasingsystem.project.domain.HistoryOrder;

import java.util.List;

public interface IHistoryOrderRepo {
    List<HistoryOrderItem> findAll();
    List<HistoryOrderItem> findByCompanyId(int companyId);
}
