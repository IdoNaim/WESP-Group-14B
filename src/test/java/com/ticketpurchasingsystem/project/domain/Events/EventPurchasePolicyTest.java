package com.ticketpurchasingsystem.project.domain.Events;

import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EventPurchasePolicyTest {

    // ================= FLAT RULES (BASIC CHECKLIST) =================

    @Test
    void GivenValidConditions_WhenValidate_ThenReturnTrue() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new MinTicketsRule(1));
        policy.addRule(new MaxTicketsRule(10));
        policy.addRule(new MinAgeRule(18));
        policy.addRule(new MaxAgeRule(60));

        // Assuming PurchaseContext was updated to drop the emptySeat boolean
        PurchaseContext context = new PurchaseContext(5, 25);

        assertTrue(policy.validate(context));
    }

    @Test
    void GivenBelowMinTickets_WhenValidate_ThenReturnFalse() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new MinTicketsRule(3));

        PurchaseContext context = new PurchaseContext(2, 25);

        assertFalse(policy.validate(context));
    }

    @Test
    void GivenAboveMaxTickets_WhenValidate_ThenReturnFalse() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new MaxTicketsRule(5));

        PurchaseContext context = new PurchaseContext(6, 25);

        assertFalse(policy.validate(context));
    }

    @Test
    void GivenBelowMinAge_WhenValidate_ThenReturnFalse() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new MinAgeRule(18));

        PurchaseContext context = new PurchaseContext(5, 16);

        assertFalse(policy.validate(context));
    }

    @Test
    void GivenAboveMaxAge_WhenValidate_ThenReturnFalse() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new MaxAgeRule(60));

        PurchaseContext context = new PurchaseContext(5, 65);

        assertFalse(policy.validate(context));
    }

    @Test
    void GivenExactMinTickets_WhenValidate_ThenReturnTrue() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new MinTicketsRule(5));

        PurchaseContext context = new PurchaseContext(5, 25);

        assertTrue(policy.validate(context));
    }

    @Test
    void GivenExactMaxTickets_WhenValidate_ThenReturnTrue() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new MaxTicketsRule(5));

        PurchaseContext context = new PurchaseContext(5, 25);

        assertTrue(policy.validate(context));
    }

    // ================= COMPOSITE LOGIC: OR RULE =================

    @Test
    void GivenOrRule_WhenOnlyOneConditionMet_ThenReturnTrue() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        // Allowed if: Under 18 OR buying 10+ tickets
        policy.addRule(new OrRule(new MaxAgeRule(17), new MinTicketsRule(10)));

        // Fails age (25 > 17), but meets tickets (15 >= 10) -> TRUE
        assertTrue(policy.validate(new PurchaseContext(15, 25)));

        // Meets age (16 <= 17), but fails tickets (2 < 10) -> TRUE
        assertTrue(policy.validate(new PurchaseContext(2, 16)));
    }

    @Test
    void GivenOrRule_WhenNeitherConditionMet_ThenReturnFalse() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new OrRule(new MaxAgeRule(17), new MinTicketsRule(10)));

        // Fails age (25) AND fails tickets (5) -> FALSE
        assertFalse(policy.validate(new PurchaseContext(5, 25)));
    }

    // ================= COMPOSITE LOGIC: AND RULE =================

    @Test
    void GivenAndRule_WhenBothConditionsMet_ThenReturnTrue() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        // Allowed if: Over 18 AND buying <= 2 tickets
        policy.addRule(new AndRule(new MinAgeRule(18), new MaxTicketsRule(2)));

        // Meets age (20) AND meets tickets (2) -> TRUE
        assertTrue(policy.validate(new PurchaseContext(2, 20)));
    }

    @Test
    void GivenAndRule_WhenOneConditionFails_ThenReturnFalse() {
        EventPurchasePolicy policy = new EventPurchasePolicy();
        policy.addRule(new AndRule(new MinAgeRule(18), new MaxTicketsRule(2)));

        // Meets age (20), but fails tickets (5) -> FALSE
        assertFalse(policy.validate(new PurchaseContext(5, 20)));

        // Fails age (15), meets tickets (1) -> FALSE
        assertFalse(policy.validate(new PurchaseContext(1, 15)));
    }

    // ================= NESTED COMPOSITE LOGIC (TREES) =================

    @Test
    void GivenNestedRules_WhenEvaluated_ThenResolveLogicCorrectly() {
        EventPurchasePolicy policy = new EventPurchasePolicy();

        // Scenario: Age must be < 18 AND (Tickets > 5 OR Tickets < 2)
        // Which translates to: MaxAge 17 AND (MinTickets 6 OR MaxTickets 1)

        IPurchaseRule ageCondition = new MaxAgeRule(17);
        IPurchaseRule quantityCondition = new OrRule(new MinTicketsRule(6), new MaxTicketsRule(1));

        policy.addRule(new AndRule(ageCondition, quantityCondition));

        // 1. Meets Age (16), meets Ticket condition 1 (7 >= 6) -> TRUE
        assertTrue(policy.validate(new PurchaseContext(7, 16)));

        // 2. Meets Age (16), meets Ticket condition 2 (1 <= 1) -> TRUE
        assertTrue(policy.validate(new PurchaseContext(1, 16)));

        // 3. Meets Age (16), fail BOTH ticket conditions (3 is between 2 and 5) -> FALSE
        assertFalse(policy.validate(new PurchaseContext(3, 16)));

        // 4. Fails Age (25), even if ticket condition met (7) -> FALSE
        assertFalse(policy.validate(new PurchaseContext(7, 25)));
    }
}