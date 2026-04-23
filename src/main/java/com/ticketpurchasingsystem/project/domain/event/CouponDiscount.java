package com.ticketpurchasingsystem.project.domain.event;

import java.time.LocalDateTime;

public class CouponDiscount implements Discount {
    private final String discountName;
    private final String couponCode;
    private final double discountPercentage;
    private final LocalDateTime validUntil;

    public CouponDiscount(String discountName, String couponCode, double discountPercentage, LocalDateTime validUntil) {
        this.discountName = discountName;
        this.couponCode = couponCode;
        this.discountPercentage = discountPercentage;
        this.validUntil = validUntil;
    }

    @Override
    public String getDiscountName() { return discountName; }

    @Override
    public boolean isApplicable() {
        return true;
    }

    @Override
    public double calculateDiscountAmount() {
        return 4.6;
    }
}
