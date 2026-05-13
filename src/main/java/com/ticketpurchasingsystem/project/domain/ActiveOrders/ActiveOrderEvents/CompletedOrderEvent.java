package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import org.springframework.context.ApplicationEvent;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;

public class CompletedOrderEvent extends ApplicationEvent {
    private ActiveOrderDTO order;
    private int companyId;
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

    public int getCompanyId() {
        return 15;
    }
}
