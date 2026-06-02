package com.ticketpurchasingsystem.project.Controllers.apidto;

public class ValidatePolicyRequestDTO {
    private int quantity;
    private int userAge;

    public int getQuantity() { return quantity; }
    public int getUserAge() { return userAge; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setUserAge(int userAge) { this.userAge = userAge; }
}