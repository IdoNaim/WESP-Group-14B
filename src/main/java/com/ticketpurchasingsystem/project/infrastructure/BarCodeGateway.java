package com.ticketpurchasingsystem.project.infrastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.ticketpurchasingsystem.project.application.IBarCodeGateway;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.BarcodeDTO;

@Component
public class BarCodeGateway implements IBarCodeGateway {

    static final String API_URL = "https://damp-lynna-wsep-1984852e.koyeb.app/";
    private static final String FAILURE = "-1";

    private final RestTemplate restTemplate;

    public BarCodeGateway(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<BarcodeDTO> issueBarcodes(ActiveOrderDTO order) {
        List<BarcodeDTO> issued = new ArrayList<>();

        for (String seatId : order.getSeatIds()) {
            String[] parts = seatId.split("_");
            String zone = parts[0];
            String row  = parts[1];
            String seat = parts[2];

            Map<String, Object> body = new HashMap<>();
            body.put("action_type", "issue ticket");
            body.put("customer_id", order.getUserId());
            body.put("event_id", order.getEventId());
            body.put("zone", zone);
            body.put("is_seating", true);
            body.put("seats", List.of(Map.of("row", row, "seat", seat)));

            String response = post(body);
            if (response == null || response.trim().equals(FAILURE)) {
                cancelTickets(issued);
                return null;
            }
            issued.add(new BarcodeDTO(response.trim()));
        }

        for (Map.Entry<String, Integer> entry : order.getStandingAreaQuantities().entrySet()) {
            Map<String, Object> body = new HashMap<>();
            body.put("action_type", "issue ticket");
            body.put("customer_id", order.getUserId());
            body.put("event_id", order.getEventId());
            body.put("zone", entry.getKey());
            body.put("quantity", entry.getValue());

            String response = post(body);
            if (response == null || response.trim().equals(FAILURE)) {
                cancelTickets(issued);
                return null;
            }
            issued.add(new BarcodeDTO(response.trim()));
        }

        return issued;
    }

    @Override
    public void cancelTickets(List<BarcodeDTO> barcodes) {
        for (BarcodeDTO barcode : barcodes) {
            Map<String, Object> body = new HashMap<>();
            body.put("action_type", "cancel ticket");
            body.put("ticket_id", barcode.getBarcodeValue());
            try {
                post(body);
            } catch (Exception ignored) {
                // best-effort cancellation
            }
        }
    }

    private String post(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForObject(API_URL, new HttpEntity<>(body, headers), String.class);
    }
}
