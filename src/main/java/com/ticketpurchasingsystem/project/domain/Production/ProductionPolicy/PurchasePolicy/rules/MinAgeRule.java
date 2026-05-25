package com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules;

import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.PurchaseContext;

public class MinAgeRule implements IPurchaseRule {
    private final int minimumAge;

    public MinAgeRule(int minimumAge) {
        this.minimumAge = minimumAge;
    }

    @Override
    public boolean validate(PurchaseContext context) {
        return context.getUserAge() >= minimumAge;
    }
}
