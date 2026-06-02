package com.ticketpurchasingsystem.project.infrastructure;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.application.IPaymentGateway;
import com.ticketpurchasingsystem.project.application.PaymentDetails;

@Component
@Profile("dev")
public class StubPaymentGateway implements IPaymentGateway {

    private final AtomicInteger nextTransactionId = new AtomicInteger(1000);

    @Override
    public int pay(PaymentDetails details) {
        return nextTransactionId.getAndIncrement();
    }

    @Override
    public int refund(int transactionId) {
        return transactionId;
    }

    @Override
    public boolean handshake() {
        return true;
    }
}
