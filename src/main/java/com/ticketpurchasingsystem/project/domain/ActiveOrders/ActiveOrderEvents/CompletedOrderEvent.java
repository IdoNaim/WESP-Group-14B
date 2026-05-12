package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import org.springframework.context.ApplicationEvent;

public class CompletedOrderEvent extends ApplicationEvent {
    private ActiveOrderDTO order;
    private double amountPaid;
    public CompletedOrderEvent(Object source, ActiveOrderDTO order, double amountPaid){
        super(source);
        this.order = order;
        this.amountPaid = amountPaid;
    }

    public ActiveOrderDTO getOrder() {
        return order;
    }

    public double getAmountPaid() {
        return amountPaid;
    }
}
