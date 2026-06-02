package com.ticketpurchasingsystem.project.application;

import com.ticketpurchasingsystem.project.domain.Utils.PaymentDetailsDTO;

public interface IPaymentGateway {

    int pay(PaymentDetails details);
    int refund(int transactionId);
    boolean handshake();
}
