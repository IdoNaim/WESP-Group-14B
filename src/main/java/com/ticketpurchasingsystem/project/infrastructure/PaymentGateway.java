package com.ticketpurchasingsystem.project.infrastructure;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.ticketpurchasingsystem.project.application.IPaymentGateway;
import com.ticketpurchasingsystem.project.application.PaymentDetails;

@Component
public class PaymentGateway implements IPaymentGateway {

    static final String API_URL = "https://damp-lynna-wsep-1984852e.koyeb.app/";

    private final RestTemplate restTemplate;

    public PaymentGateway(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public int pay(PaymentDetails details) {
        Map<String, Object> body = new HashMap<>();
        body.put("action_type", "pay");
        body.put("amount", details.getAmount());
        body.put("currency", details.getCurrency());
        body.put("card_number", details.getCardNumber());
        body.put("month", details.getMonth());
        body.put("year", details.getYear());
        body.put("holder", details.getHolder());
        body.put("cvv", details.getCvv());
        body.put("id", details.getId());
        return parseIntResponse(post(body));
    }

    @Override
    public int refund(int transactionId) {
        Map<String, Object> body = new HashMap<>();
        body.put("action_type", "refund");
        body.put("transaction_id", transactionId);
        return parseIntResponse(post(body));
    }

    @Override
    public boolean handshake() {
        Map<String, Object> body = new HashMap<>();
        body.put("action_type", "handshake");
        String response = post(body);
        return response != null && response.trim().equalsIgnoreCase("OK");
    }

    private String post(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForObject(API_URL, new HttpEntity<>(body, headers), String.class);
    }

    private int parseIntResponse(String response) {
        if (response == null) return -1;
        try {
            return Integer.parseInt(response.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
