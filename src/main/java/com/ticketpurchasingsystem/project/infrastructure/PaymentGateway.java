package com.ticketpurchasingsystem.project.infrastructure;

import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.application.IPaymentGateway;
import com.ticketpurchasingsystem.project.domain.Utils.PaymentDetailsDTO;

@Component
public class PaymentGateway implements IPaymentGateway {

    @Override
    public boolean pay(PaymentDetailsDTO paymentDetails, double amount) {
        // TODO: integrate real payment provider
        return true;
    }

    @Override
    public boolean refund(String orderID, double amount) {
        // TODO: integrate real payment provider
        return true;
    }
}
