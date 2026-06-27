package com.ticketpurchasingsystem.project.infrastructure;

import java.util.HashMap;
import java.util.Map;

import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import org.springframework.beans.factory.annotation.Value;

import com.ticketpurchasingsystem.project.application.IPaymentGateway;
import com.ticketpurchasingsystem.project.application.PaymentDetails;

@Component
@Profile("!dev")
public class PaymentGateway implements IPaymentGateway {

    private final String apiUrl;
    private final RestTemplate restTemplate;

    public PaymentGateway(@Value("${gateway.api.url}") String apiUrl, RestTemplate restTemplate) {
        this.apiUrl = apiUrl;
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

//    private String post(Map<String, Object> body) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        return restTemplate.postForObject(API_URL, new HttpEntity<>(body, headers), String.class);
//    }
    private String post(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        body.forEach((k, v) -> form.add(k, String.valueOf(v)));

        try {
            return restTemplate.postForObject(apiUrl, new HttpEntity<>(form, headers), String.class);
        } catch (Exception e) {
            loggerDef.getInstance().error("Payment gateway connection error: " + e.getMessage());
            throw new RuntimeException("Payment could not be processed. Please check your card details and try again.");
        }
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
