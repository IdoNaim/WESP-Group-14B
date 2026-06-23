package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import org.springframework.context.ApplicationEvent;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;

public class CompletedOrderEvent extends ApplicationEvent {
    private ActiveOrderDTO order;
    private int companyId;
    private double amountPaid;
    private int transactionId;

    public CompletedOrderEvent(Object source, ActiveOrderDTO order, double amountPaid, int companyId, int transactionId) {
        super(source);
        this.order = order;
        this.amountPaid = amountPaid;
        this.companyId = companyId;
        this.transactionId = transactionId;
    }

    public CompletedOrderEvent(Object source, ActiveOrderDTO order, double amountPaid, int companyId) {
        this(source, order, amountPaid, companyId, -1);
    }

    public ActiveOrderDTO getOrder() {
        return order;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public int getCompanyId() {
        return companyId;
    }

    public int getTransactionId() {
        return transactionId;
    }
}
