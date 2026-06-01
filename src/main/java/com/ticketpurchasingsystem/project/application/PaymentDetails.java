package com.ticketpurchasingsystem.project.application;

public class PaymentDetails {
    private double amount;
    private String currency;
    private String cardNumber;
    private String month;
    private String year;
    private String holder;
    private String cvv;
    private String id;

    public PaymentDetails(double amount, String currency, String cardNumber,
                          String month, String year, String holder,
                          String cvv, String id) {
        this.amount = amount;
        this.currency = currency;
        this.cardNumber = cardNumber;
        this.month = month;
        this.year = year;
        this.holder = holder;
        this.cvv = cvv;
        this.id = id;
    }

    public double getAmount()      { return amount; }
    public String getCurrency()    { return currency; }
    public String getCardNumber()  { return cardNumber; }
    public String getMonth()       { return month; }
    public String getYear()        { return year; }
    public String getHolder()      { return holder; }
    public String getCvv()         { return cvv; }
    public String getId()          { return id; }
}
