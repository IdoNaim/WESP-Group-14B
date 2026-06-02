package com.ticketpurchasingsystem.project.Controllers.apidto;

public class CheckoutRequestDTO {
    private double amount;
    private String creditCardNumber;
    private String cardHolderName;
    private String expirationDate; // Format: "MM/YY"
    private String cvv;

    // Amount
    public double getAmount() { 
        return amount; 
    }
    public void setAmount(double amount) { 
        this.amount = amount; 
    }

    // Credit Card Number
    public String getCreditCardNumber() { 
        return creditCardNumber; 
    }
    public void setCreditCardNumber(String creditCardNumber) { 
        this.creditCardNumber = creditCardNumber; 
    }

    // Card Holder Name
    public String getCardHolderName() { 
        return cardHolderName; 
    }
    public void setCardHolderName(String cardHolderName) { 
        this.cardHolderName = cardHolderName; 
    }

    // Expiration Date
    public String getExpirationDate() { 
        return expirationDate; 
    }
    public void setExpirationDate(String expirationDate) { 
        this.expirationDate = expirationDate; 
    }

    // CVV
    public String getCvv() { 
        return cvv; 
    }
    public void setCvv(String cvv) { 
        this.cvv = cvv; 
    }
}