package com.ticketpurchasingsystem.project.domain.HistoryOrder;

import java.util.List;

public interface IHistoryOrderRepo {
    HistoryOrderItem save(HistoryOrderItem historyOrder);
    List<HistoryOrderItem> findAll();
    List<HistoryOrderItem> findAllByCompanyId(int companyId);
    List<HistoryOrderItem> findAllByUserId(String userId);
    HistoryOrderItem findByOrderId(String orderId);
    List<HistoryOrderItem> findAllByEventId(String eventId);
    List<HistoryOrderItem> findAllByUserIdAndEventId(String userId, String eventId);
    void deleteAll();
}
