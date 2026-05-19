package com.ticketpurchasingsystem.project.domain.event.Purchase_Policy;

import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.PurchaseContext;

public class MinAgeRule implements IPurchaseRule {
    private final int minAge;

    public MinAgeRule(int minAge) {
        this.minAge = minAge;
    }

    @Override
    public boolean validate(PurchaseContext context) {
        return context.getUserAge() >= minAge;
    }
}