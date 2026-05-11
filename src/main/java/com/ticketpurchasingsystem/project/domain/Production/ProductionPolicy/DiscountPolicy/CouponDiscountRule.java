package com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.DiscountPolicy;

public class CouponDiscountRule implements IDiscountRule {
    private String validCode;
    private double discountPercentage;

    public CouponDiscountRule(String validCode, double discountPercentage) {
        this.validCode = validCode;
        this.discountPercentage = discountPercentage;
    }

    @Override
    public double applyDiscount(double currentPrice, DiscountContext context) {
        if (validCode.equals(context.getCouponCode())) {
            return currentPrice * (1 - (discountPercentage / 100));
        }
        return currentPrice;
    }
}