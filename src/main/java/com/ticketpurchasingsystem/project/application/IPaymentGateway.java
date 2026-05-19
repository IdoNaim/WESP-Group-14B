package com.ticketpurchasingsystem.project.application;

public interface IPaymentGateway {
    public boolean pay();
    public boolean refund(String orderID, double amount);
}
