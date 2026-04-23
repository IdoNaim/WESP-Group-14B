package com.ticketpurchasingsystem.project.domain.event;

public class DependentDiscount implements Discount {
        private final String discountName;
    private final int haveToBuyAmount;
    private final int getForFreeAmount; 

    public DependentDiscount(String discountName, int haveToBuyAmount, int getForFreeAmount) {
        this.discountName = discountName;
        this.haveToBuyAmount = haveToBuyAmount;
        this.getForFreeAmount = getForFreeAmount;
    }

    @Override
    public String getDiscountName() { return discountName; }

    @Override
    public boolean isApplicable() {
        return true;
    }

    @Override
    public double calculateDiscountAmount() {
        return 5.6;
    }
}
