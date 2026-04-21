package com.ticketpurchasingsystem.project.domain.event;

import java.time.LocalDateTime;
import java.util.Date;
/**
 * Represents the discount policy attached to an Event.
 * Holds all discounts and defines how they combine.
 */
public class EventDiscountPolicy {

}
class visibileDiscount {
    private final String discountName;
    private final double discountPercentage;
    private final Date validUntil;

    public visibileDiscount(String discountName, double discountPercentage, Date validUntil) {
        this.discountName = discountName;
        this.discountPercentage = discountPercentage;
        this.validUntil = validUntil;
    }

    public String getDiscountName() {
        return discountName;
    }

    public double getDiscountPercentage() {
        return discountPercentage;
    }

    public Date getValidUntil() {
        return validUntil;
    }
}
interface Discount {
    String getDiscountName();
    boolean isApplicable();
    double calculateDiscountAmount();
}
class VisibleDiscount implements Discount {
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

class DependentDiscount implements Discount {
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

class CouponDiscount implements Discount {
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