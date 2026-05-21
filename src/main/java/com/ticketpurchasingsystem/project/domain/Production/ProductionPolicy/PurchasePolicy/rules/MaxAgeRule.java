package com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules;

import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.PurchaseContext;

public class MaxAgeRule implements IPurchaseRule {
    private final int maximumAge;

    public MaxAgeRule(int maximumAge) {
        this.maximumAge = maximumAge;
    }

    @Override
    public boolean validate(PurchaseContext context) {
        return context.getUserAge() <= maximumAge;
    }
}
