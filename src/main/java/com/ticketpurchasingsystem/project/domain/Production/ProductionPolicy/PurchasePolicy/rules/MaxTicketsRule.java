package com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules;

import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.PurchaseContext;

public class MaxTicketsRule implements IPurchaseRule {
    private int limit;

    public MaxTicketsRule(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean validate(PurchaseContext context) {
        return context.getRequestedAmount() <= limit;
    }
}
