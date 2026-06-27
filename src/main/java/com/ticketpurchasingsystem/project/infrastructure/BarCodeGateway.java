package com.ticketpurchasingsystem.project.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import org.springframework.beans.factory.annotation.Value;

import com.ticketpurchasingsystem.project.application.IBarCodeGateway;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.BarcodeDTO;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

@Component
public class BarCodeGateway implements IBarCodeGateway {

    private final String apiUrl;
    private static final String FAILURE = "-1";

    private final RestTemplate restTemplate;

    public BarCodeGateway(@Value("${barcode.gateway.api.url}") String apiUrl, RestTemplate restTemplate) {
        this.apiUrl = apiUrl;
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

            // seats must be a JSON-serialized string per the API spec
            String seatsJson = "[{\"row\": " + row + ", \"seat\": " + seat + "}]";

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("action_type", "issue_ticket");
            form.add("customer_id", order.getUserId());
            form.add("event_id", order.getEventId());
            form.add("zone", zone);
            form.add("is_seating", "true");
            form.add("seats", seatsJson);

            String response = post(form);
            if (response == null || response.trim().equals(FAILURE)) {
                cancelTickets(issued);
                return null;
            }
            issued.add(new BarcodeDTO(response.trim()));
        }
        for (Map.Entry<String, Integer> entry : order.getStandingAreaQuantities().entrySet()) {
            int qty = entry.getValue();
            for (int i = 0; i < qty; i++) {
                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                form.add("action_type", "issue_ticket");
                form.add("customer_id", order.getUserId());
                form.add("event_id", order.getEventId());
                form.add("zone", entry.getKey());
                form.add("quantity", "1");

                String response = post(form);
                if (response == null || response.trim().equals(FAILURE)) {
                    cancelTickets(issued);
                    return null;
                }
                issued.add(new BarcodeDTO(response.trim()));
            }
        }
        return issued;
    }

    @Override
    public void cancelTickets(List<BarcodeDTO> barcodes) {
        for (BarcodeDTO barcode : barcodes) {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("action_type", "cancel_ticket");
            form.add("ticket_id", barcode.getBarcodeValue());
            try {
                post(form);
            } catch (Exception ignored) {
                // best-effort cancellation
            }
        }
    }

    private String post(MultiValueMap<String, String> form) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            return restTemplate.postForObject(apiUrl, new HttpEntity<>(form, headers), String.class);
        } catch (Exception e) {
            loggerDef.getInstance().error("Network error calling external BarCodeGateway: " + e.getMessage());
            return null;
        }
    }
}
