package com.ticketpurchasingsystem.project.domain.ActiveOrders;

import java.util.List;

public interface IActiveOrderRepo {
    List<ActiveOrderItem> findAll();
}
