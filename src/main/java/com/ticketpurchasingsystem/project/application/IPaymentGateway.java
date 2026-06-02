package com.ticketpurchasingsystem.project.application;

public interface IPaymentGateway {
    int pay(PaymentDetails details);
    int refund(int transactionId);
    boolean handshake();
}
