package com.ticketpurchasingsystem.project.application;

import com.ticketpurchasingsystem.project.domain.Utils.PaymentDetailsDTO;

public interface IPaymentGateway {
    public boolean pay(PaymentDetailsDTO paymentDetails, double amount);
    public boolean refund(String orderID, double amount);
}
