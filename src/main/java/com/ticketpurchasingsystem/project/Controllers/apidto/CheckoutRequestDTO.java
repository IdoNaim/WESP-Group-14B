package com.ticketpurchasingsystem.project.Controllers.apidto;

import com.ticketpurchasingsystem.project.application.PaymentDetails;

public class CheckoutRequestDTO {
    private double amount;

    private String currency;
    private String cardNumber;
    private String month;
    private String year;
    private String holder;
    private String cvv;
    private String id;

    public PaymentDetails toPaymentDetails() {
        return new PaymentDetails(amount, currency, cardNumber, month, year, holder, cvv, id);
    }

    public double getAmount()             { return amount; }
    public void setAmount(double amount)  { this.amount = amount; }

    public String getCurrency()                  { return currency; }
    public void setCurrency(String currency)     { this.currency = currency; }

    public String getCardNumber()                { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getMonth()             { return month; }
    public void setMonth(String month)   { this.month = month; }

    public String getYear()              { return year; }
    public void setYear(String year)     { this.year = year; }

    public String getHolder()            { return holder; }
    public void setHolder(String holder) { this.holder = holder; }

    public String getCvv()           { return cvv; }
    public void setCvv(String cvv)   { this.cvv = cvv; }

    public String getId()          { return id; }
    public void setId(String id)   { this.id = id; }
}
