package com.ticketpurchasingsystem.project.domain.Events;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.ticketpurchasingsystem.project.domain.event.EventPurchasePolicy;

public class EventPurchasePolicyTest {
    
    // ---------------- HAPPY PATH ----------------

    @Test
    void canPurchase_shouldReturnTrue_whenAllConditionsMet() {

        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 10, 18, 60, true);

        boolean result = policy.canPurchase(5, 25, false);

        assertTrue(result);
    }

    // ---------------- MIN TICKETS ----------------

    @Test
    void canPurchase_shouldReturnFalse_whenBelowMinTickets() {

        EventPurchasePolicy policy =
                new EventPurchasePolicy(3, 10, 18, 60, true);

        boolean result = policy.canPurchase(2, 25, false);

        assertFalse(result);
    }

    // ---------------- MAX TICKETS ----------------

    @Test
    void canPurchase_shouldReturnFalse_whenAboveMaxTickets() {

        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 5, 18, 60, true);

        boolean result = policy.canPurchase(6, 25, false);

        assertFalse(result);
    }

    // ---------------- MIN AGE ----------------

    @Test
    void canPurchase_shouldReturnFalse_whenBelowMinAge() {

        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 10, 18, 60, true);

        boolean result = policy.canPurchase(5, 16, false);

        assertFalse(result);
    }

    // ---------------- MAX AGE ----------------

    @Test
    void canPurchase_shouldReturnFalse_whenAboveMaxAge() {

        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 10, 18, 60, true);

        boolean result = policy.canPurchase(5, 65, false);

        assertFalse(result);
    }

    // ---------------- EMPTY SEAT RULE ----------------

    @Test
    void canPurchase_shouldReturnFalse_whenEmptySeatNotAllowed() {

        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 10, 18, 60, false);

        boolean result = policy.canPurchase(5, 25, true);

        assertFalse(result);
    }

    @Test
    void canPurchase_shouldReturnTrue_whenEmptySeatAllowed() {

        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 10, 18, 60, true);

        boolean result = policy.canPurchase(5, 25, true);

        assertTrue(result);
    }

    // ---------------- EDGE CASES ----------------

    @Test
    void canPurchase_shouldReturnTrue_whenNoTicketLimits() {

        EventPurchasePolicy policy =
                new EventPurchasePolicy(null, null, 18, 60, true);

        boolean result = policy.canPurchase(100, 25, false);

        assertTrue(result);
    }

    @Test
    void canPurchase_shouldReturnTrue_whenNoAgeLimits() {

        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 10, null, null, true);

        boolean result = policy.canPurchase(5, 5, false);

        assertTrue(result);
    }

    // ---------------- BOUNDARY VALUES ----------------

    @Test
    void canPurchase_shouldAllowExactMinTickets() {

        EventPurchasePolicy policy =
                new EventPurchasePolicy(5, 10, 18, 60, true);

        boolean result = policy.canPurchase(5, 25, false);

        assertTrue(result);
    }

    @Test
    void canPurchase_shouldAllowExactMaxTickets() {

        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 5, 18, 60, true);

        boolean result = policy.canPurchase(5, 25, false);

        assertTrue(result);
    }

    @Test
    void canPurchase_shouldAllowExactMinAge() {

        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 10, 18, 60, true);

        boolean result = policy.canPurchase(5, 18, false);

        assertTrue(result);
    }

    @Test
    void canPurchase_shouldAllowExactMaxAge() {

        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 10, 18, 60, true);

        boolean result = policy.canPurchase(5, 60, false);

        assertTrue(result);
    }
}
