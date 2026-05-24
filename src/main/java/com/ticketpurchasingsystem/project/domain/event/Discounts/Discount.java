package com.ticketpurchasingsystem.project.domain.event;

interface Discount {
    String getDiscountName();
    boolean isApplicable();
    double calculateDiscountAmount();
}
