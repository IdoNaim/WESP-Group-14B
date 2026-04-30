package com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy;

public class PurchaseContext {
    private int requestedAmount;
    private int userAge;

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
