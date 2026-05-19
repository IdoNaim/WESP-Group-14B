package com.ticketpurchasingsystem.project.domain.event.Purchase_Policy;

import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.PurchaseContext;

public class MinTicketsRule implements IPurchaseRule {
    private final int limit;

    public MinTicketsRule(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean validate(PurchaseContext context) {
        return context.getRequestedAmount() >= limit;
    }
}