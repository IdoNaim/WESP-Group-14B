package com.ticketpurchasingsystem.project.domain.User;

public enum UserGroupDiscount {
    NONE(0),
    STUDENT(10),
    SENIOR(15),
    VETERAN(20);

    private final int discountPercentage;

    UserGroupDiscount(int discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public int getDiscountPercentage() {
        return discountPercentage;
    }
}
