package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import java.util.List;

import org.springframework.context.ApplicationEvent;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.BarcodeDTO;

public class CompletedOrderEvent extends ApplicationEvent {
    private ActiveOrderDTO order;
    private int companyId;
    private double amountPaid;
    private int transactionId;
    private List<BarcodeDTO> barcodes;

    public CompletedOrderEvent(Object source, ActiveOrderDTO order, double amountPaid, int companyId, int transactionId, List<BarcodeDTO> barcodes) {
        super(source);
        this.order = order;
        this.amountPaid = amountPaid;
        this.companyId = companyId;
        this.transactionId = transactionId;
        this.barcodes = barcodes;
    }

    public CompletedOrderEvent(Object source, ActiveOrderDTO order, double amountPaid, int companyId, int transactionId) {
        this(source, order, amountPaid, companyId, transactionId, null);
    }

    public CompletedOrderEvent(Object source, ActiveOrderDTO order, double amountPaid, int companyId) {
        this(source, order, amountPaid, companyId, -1, null);
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

    public List<BarcodeDTO> getBarcodes() {
        return barcodes;
    }
}
