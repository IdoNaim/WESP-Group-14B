package com.ticketpurchasingsystem.project.infrastructure;

import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.application.IPaymentGateway;

@Component
public class PaymentGateway implements IPaymentGateway {

    @Override
    public boolean pay() {
        // TODO: integrate real payment provider
        return true;
    }

    @Override
    public boolean refund(String orderID, double amount) {
        // TODO: integrate real payment provider
        return true;
    }
}
