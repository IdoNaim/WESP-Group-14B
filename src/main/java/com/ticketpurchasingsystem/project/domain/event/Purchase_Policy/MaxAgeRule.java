package com.ticketpurchasingsystem.project.domain.event.Purchase_Policy;

import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.PurchaseContext;

public class MaxAgeRule implements IPurchaseRule {
    private final int maxAge;

    public MaxAgeRule(int maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    public boolean validate(PurchaseContext context) {
        return context.getUserAge() <= maxAge;
    }
}
