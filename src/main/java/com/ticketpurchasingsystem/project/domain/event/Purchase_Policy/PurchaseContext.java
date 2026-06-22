package com.ticketpurchasingsystem.project.domain.event.Purchase_Policy;

public class PurchaseContext {
    private int requestedAmount;
    private int userAge;
    private int alreadyPurchased;

    public PurchaseContext(int amount, int age) {
        this(amount, age, 0);
    }

    public PurchaseContext(int amount, int age, int alreadyPurchased) {
        this.requestedAmount = amount;
        this.userAge = age;
        this.alreadyPurchased = alreadyPurchased;
    }

    public int getRequestedAmount() {
        return requestedAmount;
    }

    public int getUserAge() {
        return userAge;
    }

    public int getAlreadyPurchased() {
        return alreadyPurchased;
    }

}