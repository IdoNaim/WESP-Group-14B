package com.ticketpurchasingsystem.project.domain.ActiveOrders;
import java.util.List;

public interface  IActiveOrderRepo {
    public boolean save(ActiveOrderItem order);
    public ActiveOrderItem findById(String orderId);
    public void delete(String orderId);
    public void update(ActiveOrderItem order);
    public List<ActiveOrderItem> findAll();
    public ActiveOrderItem findByUserId(String userId);
}
