package com.ticketpurchasingsystem.project.domain.ActiveOrders;

public interface  IActiveOrderRepo {
    public boolean save(ActiveOrderItem order);
    public ActiveOrderItem findById(int orderId);
    public void delete(int orderId);
    public void update(ActiveOrderItem order);

}
