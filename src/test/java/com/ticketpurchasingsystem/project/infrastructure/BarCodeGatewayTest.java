package com.ticketpurchasingsystem.project.infrastructure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.BarcodeDTO;

@ExtendWith(MockitoExtension.class)
class BarCodeGatewayTest {

    @Mock
    private RestTemplate restTemplate;

    private BarCodeGateway barCodeGateway;

    @BeforeEach
    void setUp() {
        barCodeGateway = new BarCodeGateway(restTemplate);
    }

    @Test
    void givenValidActiveOrder_whenIssuingBarcodesAndAllRequestsSucceed_thenAllBarcodesAreReturned() {
        when(restTemplate.postForObject(eq(BarCodeGateway.API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("TIX-111")
                .thenReturn("TIX-222");

        List<String> seatIds = List.of("VIP_1_1");
        HashMap<String, Integer> standingQuantities = new HashMap<>();
        standingQuantities.put("StandingZone", 1);

        ActiveOrderDTO order = new ActiveOrderDTO(
                "order-1", "user-1", "event-1",
                new Timestamp(System.currentTimeMillis()), seatIds, standingQuantities
        );

        List<BarcodeDTO> result = barCodeGateway.issueBarcodes(order);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("TIX-111", result.get(0).getBarcodeValue());
        assertEquals("TIX-222", result.get(1).getBarcodeValue());

        verify(restTemplate, times(2)).postForObject(eq(BarCodeGateway.API_URL), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void givenValidActiveOrder_whenASeatingRequestFails_thenReturnsNullAndCancelsAlreadyIssuedTickets() {
        when(restTemplate.postForObject(eq(BarCodeGateway.API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("TIX-111")
                .thenReturn("-1");

        List<String> seatIds = List.of("VIP_1_1", "VIP_1_2");
        ActiveOrderDTO order = new ActiveOrderDTO(
                "order-1", "user-1", "event-1",
                new Timestamp(System.currentTimeMillis()), seatIds, new HashMap<>()
        );

        List<BarcodeDTO> result = barCodeGateway.issueBarcodes(order);

        assertNull(result);
        // Verify cancel ticket was triggered for TIX-111
        verify(restTemplate).postForObject(
                eq(BarCodeGateway.API_URL),
                argThat((HttpEntity entity) -> {
                    java.util.Map<?, ?> body = (java.util.Map<?, ?>) entity.getBody();
                    return body != null &&
                            "cancel_ticket".equals(body.get("action_type")) &&
                            "TIX-111".equals(body.get("ticket_id"));
                }),
                eq(String.class)
        );
    }

    @Test
    void givenExternalNetworkTimeout_whenIssuingBarcodes_thenReturnNull() {
        when(restTemplate.postForObject(eq(BarCodeGateway.API_URL), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection Timeout"));

        List<String> seatIds = List.of("VIP_1_1");
        ActiveOrderDTO order = new ActiveOrderDTO(
                "order-1", "user-1", "event-1",
                new Timestamp(System.currentTimeMillis()), seatIds, new HashMap<>()
        );

        List<BarcodeDTO> result = barCodeGateway.issueBarcodes(order);

        assertNull(result);
    }

    @Test
    void givenIssuedBarcodeList_whenCancelingTickets_thenCorrectPostRequestsAreSent() {
        barCodeGateway.cancelTickets(List.of(new BarcodeDTO("TIX-999")));

        verify(restTemplate).postForObject(
                eq(BarCodeGateway.API_URL),
                argThat((HttpEntity entity) -> {
                    java.util.Map<?, ?> body = (java.util.Map<?, ?>) entity.getBody();
                    return body != null &&
                            "cancel_ticket".equals(body.get("action_type")) &&
                            "TIX-999".equals(body.get("ticket_id"));
                }),
                eq(String.class)
        );
    }
}
