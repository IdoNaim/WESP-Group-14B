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

    @Mock
    private RestTemplate restTemplate;

    private PaymentGateway paymentGateway;

    private PaymentDetails validDetails;

    @BeforeEach
    void setUp() {
        paymentGateway = new PaymentGateway(restTemplate);
        validDetails = new PaymentDetails(
                100.0, "USD", "4111111111111111",
                "12", "2028", "Alice Smith", "123", "ID123"
        );
    }

    // ─── handshake ────────────────────────────────────────────────────────────

    @Test
    void handshake_returnsTrue_whenResponseIsOK() {
        when(restTemplate.postForObject(eq(PaymentGateway.API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("OK");
        assertTrue(paymentGateway.handshake());
    }

    @Test
    void handshake_returnsTrue_whenResponseIsOKCaseInsensitive() {
        when(restTemplate.postForObject(eq(PaymentGateway.API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("ok");
        assertTrue(paymentGateway.handshake());
    }

    @Test
    void handshake_returnsTrue_whenResponseHasWhitespace() {
        when(restTemplate.postForObject(eq(PaymentGateway.API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("  OK  ");
        assertTrue(paymentGateway.handshake());
    }

    @Test
    void handshake_returnsFalse_whenResponseIsNotOK() {
        when(restTemplate.postForObject(eq(PaymentGateway.API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("ERROR");
        assertFalse(paymentGateway.handshake());
    }

    @Test
    void handshake_returnsFalse_whenResponseIsNull() {
        when(restTemplate.postForObject(eq(PaymentGateway.API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(null);
        assertFalse(paymentGateway.handshake());
    }

    // ─── pay ─────────────────────────────────────────────────────────────────

    @Test
    void pay_returnsTransactionId_whenResponseIsValidInt() {
        when(restTemplate.postForObject(eq(PaymentGateway.API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("42");
        assertEquals(42, paymentGateway.pay(validDetails));
    }

    @Test
    void pay_returnsNegativeOne_whenResponseIsNull() {
        when(restTemplate.postForObject(eq(PaymentGateway.API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(null);
        assertEquals(-1, paymentGateway.pay(validDetails));
    }

    @Test
    void pay_returnsNegativeOne_whenResponseIsNotAnInt() {
        when(restTemplate.postForObject(eq(PaymentGateway.API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("error");
        assertEquals(-1, paymentGateway.pay(validDetails));
    }

    @Test
    void pay_returnsNegativeOne_whenResponseIsEmpty() {
        when(restTemplate.postForObject(eq(PaymentGateway.API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("");
        assertEquals(-1, paymentGateway.pay(validDetails));
    }

    // ─── refund ───────────────────────────────────────────────────────────────

    @Test
    void refund_returnsTransactionId_whenResponseIsValidInt() {
        when(restTemplate.postForObject(eq(PaymentGateway.API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("7");
        assertEquals(7, paymentGateway.refund(42));
    }

    @Test
    void refund_returnsNegativeOne_whenResponseIsNull() {
        when(restTemplate.postForObject(eq(PaymentGateway.API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(null);
        assertEquals(-1, paymentGateway.refund(42));
    }

    @Test
    void refund_returnsNegativeOne_whenResponseIsNotAnInt() {
        when(restTemplate.postForObject(eq(PaymentGateway.API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn("FAILED");
        assertEquals(-1, paymentGateway.refund(42));
    }
}
