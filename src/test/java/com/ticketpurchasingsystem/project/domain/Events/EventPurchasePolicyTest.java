package com.ticketpurchasingsystem.project.domain.Events;

import com.ticketpurchasingsystem.project.domain.event.EventPurchasePolicy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;;

public class EventPurchasePolicyTest {
    
 @Test
    void GivenValidConditions_WhenCanPurchase_ThenReturnTrue() {
        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 10, 18, 60, true);

        boolean result = policy.canPurchase(5, 25, false);

        assertTrue(result);
    }

    @Test
    void GivenBelowMinTickets_WhenCanPurchase_ThenReturnFalse() {
        EventPurchasePolicy policy =
                new EventPurchasePolicy(3, 10, 18, 60, true);

        assertFalse(policy.canPurchase(2, 25, false));
    }

    @Test
    void GivenAboveMaxTickets_WhenCanPurchase_ThenReturnFalse() {
        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 5, 18, 60, true);

        assertFalse(policy.canPurchase(6, 25, false));
    }

    @Test
    void GivenBelowMinAge_WhenCanPurchase_ThenReturnFalse() {
        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 10, 18, 60, true);

        assertFalse(policy.canPurchase(5, 16, false));
    }

    @Test
    void GivenAboveMaxAge_WhenCanPurchase_ThenReturnFalse() {
        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 10, 18, 60, true);

        assertFalse(policy.canPurchase(5, 65, false));
    }

    @Test
    void GivenEmptySeatNotAllowed_WhenCanPurchase_ThenReturnFalse() {
        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 10, 18, 60, false);

        assertFalse(policy.canPurchase(5, 25, true));
    }

    @Test
    void GivenEmptySeatAllowed_WhenCanPurchase_ThenReturnTrue() {
        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 10, 18, 60, true);

        assertTrue(policy.canPurchase(5, 25, true));
    }

    @Test
    void GivenNoTicketLimits_WhenCanPurchase_ThenReturnTrue() {
        EventPurchasePolicy policy =
                new EventPurchasePolicy(null, null, 18, 60, true);

        assertTrue(policy.canPurchase(100, 25, false));
    }

    @Test
    void GivenNoAgeLimits_WhenCanPurchase_ThenReturnTrue() {
        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 10, null, null, true);

        assertTrue(policy.canPurchase(5, 5, false));
    }

    @Test
    void GivenExactMinTickets_WhenCanPurchase_ThenReturnTrue() {
        EventPurchasePolicy policy =
                new EventPurchasePolicy(5, 10, 18, 60, true);

        assertTrue(policy.canPurchase(5, 25, false));
    }

    @Test
    void GivenExactMaxTickets_WhenCanPurchase_ThenReturnTrue() {
        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 5, 18, 60, true);

        assertTrue(policy.canPurchase(5, 25, false));
    }

    @Test
    void GivenExactMinAge_WhenCanPurchase_ThenReturnTrue() {
        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 10, 18, 60, true);

        assertTrue(policy.canPurchase(5, 18, false));
    }

    @Test
    void GivenExactMaxAge_WhenCanPurchase_ThenReturnTrue() {
        EventPurchasePolicy policy =
                new EventPurchasePolicy(1, 10, 18, 60, true);

        assertTrue(policy.canPurchase(5, 60, false));
    }
}
