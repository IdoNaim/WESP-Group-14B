package com.ticketpurchasingsystem.project.domain.event.Purchase_Policy;

public class MinTicketsRule implements IPurchaseRule {
    private final int limit;

    public MinTicketsRule(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean validate(PurchaseContext context) {
        return context.getRequestedAmount() >= limit;
    }
    public int getLimit() {
        return limit;
    }
}