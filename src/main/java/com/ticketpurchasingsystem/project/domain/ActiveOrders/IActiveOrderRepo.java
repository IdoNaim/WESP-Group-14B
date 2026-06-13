package com.ticketpurchasingsystem.project.domain.ActiveOrders;

import java.util.List;
import java.util.Optional;

public interface IActiveOrderRepo {
    ActiveOrderItem save(ActiveOrderItem order);
    Optional<ActiveOrderItem> findById(String orderId);
    void delete(String orderId);
    void update(ActiveOrderItem order);
    List<ActiveOrderItem> findAll();
    ActiveOrderItem findByUserId(String userId);
    boolean markAsProcessing(String orderId);
}
