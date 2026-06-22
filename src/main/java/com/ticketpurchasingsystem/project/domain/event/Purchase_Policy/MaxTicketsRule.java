package com.ticketpurchasingsystem.project.domain.event.Purchase_Policy;


public class MaxTicketsRule implements IPurchaseRule {
    private final int limit;

    public MaxTicketsRule(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean validate(PurchaseContext context) {
        return context.getAlreadyPurchased() + context.getRequestedAmount() <= limit;
    }
    public int getLimit() {
        return limit;
    }
}