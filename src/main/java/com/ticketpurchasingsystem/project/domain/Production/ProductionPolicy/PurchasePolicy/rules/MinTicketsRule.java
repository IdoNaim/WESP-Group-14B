package com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules;

import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.PurchaseContext;

public class MinTicketsRule implements IPurchaseRule {
    private final int minimum;

    public MinTicketsRule(int minimum) {
        this.minimum = minimum;
    }

    public int getMinimum() { return minimum; }

    @Override
    public boolean validate(PurchaseContext context) {
        return context.getRequestedAmount() >= minimum;
    }
}
