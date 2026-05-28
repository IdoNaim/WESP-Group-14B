package com.ticketpurchasingsystem.project.domain.event.Purchase_Policy;

public class PurchaseContext {
    private int requestedAmount;
    private int userAge;

    // Update the constructor
    public PurchaseContext(int amount, int age) {
        this.requestedAmount = amount;
        this.userAge = age;
    }

    public int getRequestedAmount() {
        return requestedAmount;
    }

    public int getUserAge() {
        return userAge;
    }

}