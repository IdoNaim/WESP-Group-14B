package com.ticketpurchasingsystem.project.infrastructure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import com.ticketpurchasingsystem.project.application.PaymentDetails;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayTest {

    private static final String TEST_URL = "https://damp-lynna-wsep-1984852e.koyeb.app/";

    @Mock
    private RestTemplate restTemplate;

    private PaymentGateway paymentGateway;

    private PaymentDetails validDetails;

    @BeforeEach
    void setUp() {
        paymentGateway = new PaymentGateway(TEST_URL, restTemplate);
        validDetails = new PaymentDetails(
                100.0, "USD", "4111111111111111",
                "12", "2028", "Alice Smith", "123", "ID123"
        );
    }

    // ─── handshake ────────────────────────────────────────────────────────────

    @Test
    void GivenOKResponse_WhenHandshake_ThenReturnTrue() {
        when(restTemplate.postForObject(eq(TEST_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("OK");
        assertTrue(paymentGateway.handshake());
    }

    @Test
    void GivenOKResponseCaseInsensitive_WhenHandshake_ThenReturnTrue() {
        when(restTemplate.postForObject(eq(TEST_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("ok");
        assertTrue(paymentGateway.handshake());
    }

    @Test
    void GivenOKResponseWithWhitespace_WhenHandshake_ThenReturnTrue() {
        when(restTemplate.postForObject(eq(TEST_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("  OK  ");
        assertTrue(paymentGateway.handshake());
    }

    @Test
    void GivenNonOKResponse_WhenHandshake_ThenReturnFalse() {
        when(restTemplate.postForObject(eq(TEST_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("ERROR");
        assertFalse(paymentGateway.handshake());
    }

    @Test
    void GivenNullResponse_WhenHandshake_ThenReturnFalse() {
        when(restTemplate.postForObject(eq(TEST_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(null);
        assertFalse(paymentGateway.handshake());
    }

    // ─── pay ─────────────────────────────────────────────────────────────────

    @Test
    void GivenValidIntResponse_WhenPay_ThenReturnTransactionId() {
        when(restTemplate.postForObject(eq(TEST_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("42");
        assertEquals(42, paymentGateway.pay(validDetails));
    }

    @Test
    void GivenNullResponse_WhenPay_ThenReturnNegativeOne() {
        when(restTemplate.postForObject(eq(TEST_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(null);
        assertEquals(-1, paymentGateway.pay(validDetails));
    }

    @Test
    void GivenNonIntResponse_WhenPay_ThenReturnNegativeOne() {
        when(restTemplate.postForObject(eq(TEST_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("error");
        assertEquals(-1, paymentGateway.pay(validDetails));
    }

    @Test
    void GivenEmptyResponse_WhenPay_ThenReturnNegativeOne() {
        when(restTemplate.postForObject(eq(TEST_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("");
        assertEquals(-1, paymentGateway.pay(validDetails));
    }

    // ─── refund ───────────────────────────────────────────────────────────────

    @Test
    void GivenValidIntResponse_WhenRefund_ThenReturnTransactionId() {
        when(restTemplate.postForObject(eq(TEST_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("7");
        assertEquals(7, paymentGateway.refund(42));
    }

    @Test
    void GivenNullResponse_WhenRefund_ThenReturnNegativeOne() {
        when(restTemplate.postForObject(eq(TEST_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(null);
        assertEquals(-1, paymentGateway.refund(42));
    }

    @Test
    void GivenNonIntResponse_WhenRefund_ThenReturnNegativeOne() {
        when(restTemplate.postForObject(eq(TEST_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("FAILED");
        assertEquals(-1, paymentGateway.refund(42));
    }
}
