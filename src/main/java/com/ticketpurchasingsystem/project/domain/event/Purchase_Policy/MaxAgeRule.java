package com.ticketpurchasingsystem.project.domain.event.Purchase_Policy;


public class MaxAgeRule implements IPurchaseRule {
    private final int maxAge;

    public MaxAgeRule(int maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    public boolean validate(PurchaseContext context) {
        return context.getUserAge() <= maxAge;
    }
    public int getMaxAge() {
        return maxAge;
    }
    public boolean validateTicketPolicy(PurchaseContext context){
        return true;
    }
}
