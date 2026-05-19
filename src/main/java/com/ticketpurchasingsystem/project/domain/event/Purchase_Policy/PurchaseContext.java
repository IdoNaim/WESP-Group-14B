package com.ticketpurchasingsystem.project.domain.event.Purchase_Policy;

public class PurchaseContext {
    private int requestedAmount;
    private int userAge;
    private boolean isSeatEmpty; // Add this field

    // Update the constructor
    public PurchaseContext(int amount, int age, boolean isSeatEmpty) {
        this.requestedAmount = amount;
        this.userAge = age;
        this.isSeatEmpty = isSeatEmpty;
    }

    public int getRequestedAmount() {
        return requestedAmount;
    }

    public int getUserAge() {
        return userAge;
    }

    // Add this getter so EmptySeatRule can use it
    public boolean isSeatEmpty() {
        return isSeatEmpty;
    }
}