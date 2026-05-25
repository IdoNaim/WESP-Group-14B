package com.ticketpurchasingsystem.project.domain.Events;

// Explicitly import your Event-specific policy and leaf rules
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.*;

// EXPLICIT IMPORT: Tell Java to use YOUR 3-argument PurchaseContext
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EventPurchasePolicyTest {

    @Test
    void GivenValidConditions_WhenValidate_ThenReturnTrue() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new MinTicketsRule(1));
        policy.addRule(new MaxTicketsRule(10)); // Resolves perfectly via explicit import
        policy.addRule(new MinAgeRule(18));
        policy.addRule(new MaxAgeRule(60));
        policy.addRule(new EmptySeatRule(true));

        // Correctly resolves to your 3-argument constructor
        PurchaseContext context = new PurchaseContext(5, 25, false);

        assertTrue(policy.validate(context));
    }

    @Test
    void GivenBelowMinTickets_WhenValidate_ThenReturnFalse() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new MinTicketsRule(3));

        PurchaseContext context = new PurchaseContext(2, 25, false);

        assertFalse(policy.validate(context));
    }

    @Test
    void GivenAboveMaxTickets_WhenValidate_ThenReturnFalse() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new MaxTicketsRule(5));

        PurchaseContext context = new PurchaseContext(6, 25, false);

        assertFalse(policy.validate(context));
    }

    @Test
    void GivenBelowMinAge_WhenValidate_ThenReturnFalse() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new MinAgeRule(18));

        PurchaseContext context = new PurchaseContext(5, 16, false);

        assertFalse(policy.validate(context));
    }

    @Test
    void GivenAboveMaxAge_WhenValidate_ThenReturnFalse() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new MaxAgeRule(60));

        PurchaseContext context = new PurchaseContext(5, 65, false);

        assertFalse(policy.validate(context));
    }

    @Test
    void GivenEmptySeatNotAllowed_WhenValidate_ThenReturnFalse() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new EmptySeatRule(false));

        PurchaseContext context = new PurchaseContext(5, 25, true);

        assertFalse(policy.validate(context));
    }

    @Test
    void GivenEmptySeatAllowed_WhenValidate_ThenReturnTrue() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new EmptySeatRule(true));

        PurchaseContext context = new PurchaseContext(5, 25, true);

        assertTrue(policy.validate(context));
    }

    @Test
    void GivenExactMinTickets_WhenValidate_ThenReturnTrue() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new MinTicketsRule(5));

        PurchaseContext context = new PurchaseContext(5, 25, false);

        assertTrue(policy.validate(context));
    }

    @Test
    void GivenExactMaxTickets_WhenValidate_ThenReturnTrue() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new MaxTicketsRule(5));

        PurchaseContext context = new PurchaseContext(5, 25, false);

        assertTrue(policy.validate(context));
    }

    @Test
    void GivenExactMinAge_WhenValidate_ThenReturnTrue() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new MinAgeRule(18));

        PurchaseContext context = new PurchaseContext(5, 18, false);

        assertTrue(policy.validate(context));
    }

    @Test
    void GivenExactMaxAge_WhenValidate_ThenReturnTrue() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new MaxAgeRule(60));

        PurchaseContext context = new PurchaseContext(5, 60, false);

        assertTrue(policy.validate(context));
    }
}