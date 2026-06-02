package com.ticketpurchasingsystem.project.domain.event.Purchase_Policy;

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