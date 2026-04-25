package com.ticketpurchasingsystem.project.domain.event;
import java.time.LocalDateTime;

public class VisibleDiscount implements Discount {
    private final String discountName;
    private final double discountPercentage;
    private final LocalDateTime validUntil;

    public VisibleDiscount(String discountName, double discountPercentage, LocalDateTime validUntil) {
        this.discountName = discountName;
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
        return discountPercentage;
    }
}
