package com.ticketpurchasingsystem.project.domain.event.Discounts;

public interface Discount {
    String getDiscountName();
    boolean isApplicable();
    double calculateDiscountAmount();
}
