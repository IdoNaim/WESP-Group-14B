package com.ticketpurchasingsystem.project.domain.ProductionTest;

import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.PurchaseContext;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.PurchasePolicy;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.AndRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.MaxAgeRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.MaxTicketsRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.MinAgeRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.MinTicketsRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.OrRule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProductionPurchasePolicyTest {

    // MaxTicketsRule

    @Test
    void GivenMaxTicketsRule_WhenAmountAtLimit_ThenReturnTrue() {
        assertTrue(new MaxTicketsRule(5).validate(new PurchaseContext(5, 25)));
    }

    @Test
    void GivenMaxTicketsRule_WhenAmountExceedsLimit_ThenReturnFalse() {
        assertFalse(new MaxTicketsRule(5).validate(new PurchaseContext(6, 25)));
    }

    // MinTicketsRule

    @Test
    void GivenMinTicketsRule_WhenAmountAtMinimum_ThenReturnTrue() {
        assertTrue(new MinTicketsRule(2).validate(new PurchaseContext(2, 25)));
    }

    @Test
    void GivenMinTicketsRule_WhenAmountBelowMinimum_ThenReturnFalse() {
        assertFalse(new MinTicketsRule(3).validate(new PurchaseContext(2, 25)));
    }

    // MinAgeRule

    @Test
    void GivenMinAgeRule_WhenAgeAtMinimum_ThenReturnTrue() {
        assertTrue(new MinAgeRule(18).validate(new PurchaseContext(1, 18)));
    }

    @Test
    void GivenMinAgeRule_WhenAgeBelowMinimum_ThenReturnFalse() {
        assertFalse(new MinAgeRule(18).validate(new PurchaseContext(1, 17)));
    }

    // MaxAgeRule

    @Test
    void GivenMaxAgeRule_WhenAgeAtMaximum_ThenReturnTrue() {
        assertTrue(new MaxAgeRule(60).validate(new PurchaseContext(1, 60)));
    }

    @Test
    void GivenMaxAgeRule_WhenAgeAboveMaximum_ThenReturnFalse() {
        assertFalse(new MaxAgeRule(60).validate(new PurchaseContext(1, 61)));
    }

    // AndRule

    @Test
    void GivenAndRule_WhenAllRulesPass_ThenReturnTrue() {
        IPurchaseRule rule = new AndRule(new MinTicketsRule(2), new MaxTicketsRule(5));
        assertTrue(rule.validate(new PurchaseContext(3, 25)));
    }

    @Test
    void GivenAndRule_WhenFirstRuleFails_ThenReturnFalse() {
        IPurchaseRule rule = new AndRule(new MinTicketsRule(2), new MaxTicketsRule(5));
        assertFalse(rule.validate(new PurchaseContext(1, 25)));
    }

    @Test
    void GivenAndRule_WhenSecondRuleFails_ThenReturnFalse() {
        IPurchaseRule rule = new AndRule(new MinTicketsRule(2), new MaxTicketsRule(5));
        assertFalse(rule.validate(new PurchaseContext(6, 25)));
    }

    // OrRule

    @Test
    void GivenOrRule_WhenFirstRulePasses_ThenReturnTrue() {
        IPurchaseRule rule = new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100));
        assertTrue(rule.validate(new PurchaseContext(2, 25)));
    }

    @Test
    void GivenOrRule_WhenSecondRulePasses_ThenReturnTrue() {
        IPurchaseRule rule = new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100));
        assertTrue(rule.validate(new PurchaseContext(100, 25)));
    }

    @Test
    void GivenOrRule_WhenAllRulesFail_ThenReturnFalse() {
        IPurchaseRule rule = new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100));
        assertFalse(rule.validate(new PurchaseContext(50, 25)));
    }

    // AND(minAge, OR(maxTickets, minTickets))

    @Test
    void GivenNestedComposite_WhenAdultBuysSmallAmount_ThenReturnTrue() {
        // "From age 18 AND (up to 2 tickets OR at least 100 tickets)"
        IPurchaseRule rule = new AndRule(
                new MinAgeRule(18),
                new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100)));
        assertTrue(rule.validate(new PurchaseContext(1, 20)));
    }

    @Test
    void GivenNestedComposite_WhenAdultBuysLargeAmount_ThenReturnTrue() {
        IPurchaseRule rule = new AndRule(
                new MinAgeRule(18),
                new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100)));
        assertTrue(rule.validate(new PurchaseContext(100, 20)));
    }

    @Test
    void GivenNestedComposite_WhenAdultBuysMidAmount_ThenReturnFalse() {
        IPurchaseRule rule = new AndRule(
                new MinAgeRule(18),
                new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100)));
        assertFalse(rule.validate(new PurchaseContext(50, 20)));
    }

    @Test
    void GivenNestedComposite_WhenUnderageUser_ThenReturnFalse() {
        IPurchaseRule rule = new AndRule(
                new MinAgeRule(18),
                new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100)));
        assertFalse(rule.validate(new PurchaseContext(1, 16)));
    }

    @Test
    void GivenEmptyPolicy_WhenValidate_ThenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy();
        assertTrue(policy.validate(new PurchaseContext(10, 25)));
    }

    @Test
    void GivenPolicy_WhenAllRulesPass_ThenReturnTrue() {
        PurchasePolicy policy = new PurchasePolicy();
        policy.addRule(new MinAgeRule(18));
        policy.addRule(new MaxTicketsRule(5));
        assertTrue(policy.validate(new PurchaseContext(3, 20)));
    }

    @Test
    void GivenPolicy_WhenOneRuleFails_ThenReturnFalse() {
        PurchasePolicy policy = new PurchasePolicy();
        policy.addRule(new MinAgeRule(18));
        policy.addRule(new MaxTicketsRule(5));
        assertFalse(policy.validate(new PurchaseContext(3, 16)));
    }

    @Test
    void GivenPolicy_WithCompositeOrRule_WhenValidate_ThenEvaluatesCorrectly() {
        PurchasePolicy policy = new PurchasePolicy();
        policy.addRule(new MinAgeRule(18));
        policy.addRule(new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100)));

        assertFalse(policy.validate(new PurchaseContext(50, 20)));
        assertTrue(policy.validate(new PurchaseContext(1, 20)));
        assertTrue(policy.validate(new PurchaseContext(100, 20)));
    }
}
