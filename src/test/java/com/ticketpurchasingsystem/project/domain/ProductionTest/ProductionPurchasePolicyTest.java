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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ProductionPurchasePolicyTest {

    // MaxTicketsRule

    @Test
    void GivenMaxTicketsRule_WhenAmountAtLimit_ThenReturnTrue() {
        // Arrange
        IPurchaseRule rule = new MaxTicketsRule(5);
        PurchaseContext context = new PurchaseContext(5, 25);

        // Act
        boolean result = rule.validate(context);

        // Assert
        assertTrue(result);
    }

    @Test
    void GivenMaxTicketsRule_WhenAmountExceedsLimit_ThenReturnFalse() {
        // Arrange
        IPurchaseRule rule = new MaxTicketsRule(5);
        PurchaseContext context = new PurchaseContext(6, 25);

        // Act
        boolean result = rule.validate(context);

        // Assert
        assertFalse(result);
    }

    // MinTicketsRule

    @Test
    void GivenMinTicketsRule_WhenAmountAtMinimum_ThenReturnTrue() {
        // Arrange
        IPurchaseRule rule = new MinTicketsRule(2);
        PurchaseContext context = new PurchaseContext(2, 25);

        // Act
        boolean result = rule.validate(context);

        // Assert
        assertTrue(result);
    }

    @Test
    void GivenMinTicketsRule_WhenAmountBelowMinimum_ThenReturnFalse() {
        // Arrange
        IPurchaseRule rule = new MinTicketsRule(3);
        PurchaseContext context = new PurchaseContext(2, 25);

        // Act
        boolean result = rule.validate(context);

        // Assert
        assertFalse(result);
    }

    // MinAgeRule

    @Test
    void GivenMinAgeRule_WhenAgeAtMinimum_ThenReturnTrue() {
        // Arrange
        IPurchaseRule rule = new MinAgeRule(18);
        PurchaseContext context = new PurchaseContext(1, 18);

        // Act
        boolean result = rule.validate(context);

        // Assert
        assertTrue(result);
    }

    @Test
    void GivenMinAgeRule_WhenAgeBelowMinimum_ThenReturnFalse() {
        // Arrange
        IPurchaseRule rule = new MinAgeRule(18);
        PurchaseContext context = new PurchaseContext(1, 17);

        // Act
        boolean result = rule.validate(context);

        // Assert
        assertFalse(result);
    }

    // MaxAgeRule

    @Test
    void GivenMaxAgeRule_WhenAgeAtMaximum_ThenReturnTrue() {
        // Arrange
        IPurchaseRule rule = new MaxAgeRule(60);
        PurchaseContext context = new PurchaseContext(1, 60);

        // Act
        boolean result = rule.validate(context);

        // Assert
        assertTrue(result);
    }

    @Test
    void GivenMaxAgeRule_WhenAgeAboveMaximum_ThenReturnFalse() {
        // Arrange
        IPurchaseRule rule = new MaxAgeRule(60);
        PurchaseContext context = new PurchaseContext(1, 61);

        // Act
        boolean result = rule.validate(context);

        // Assert
        assertFalse(result);
    }

    // AndRule

    @Test
    void GivenAndRule_WhenAllRulesPass_ThenReturnTrue() {
        // Arrange
        IPurchaseRule rule = new AndRule(new MinTicketsRule(2), new MaxTicketsRule(5));
        PurchaseContext context = new PurchaseContext(3, 25);

        // Act
        boolean result = rule.validate(context);

        // Assert
        assertTrue(result);
    }

    @Test
    void GivenAndRule_WhenFirstRuleFails_ThenReturnFalse() {
        // Arrange
        IPurchaseRule rule = new AndRule(new MinTicketsRule(2), new MaxTicketsRule(5));
        PurchaseContext context = new PurchaseContext(1, 25);

        // Act
        boolean result = rule.validate(context);

        // Assert
        assertFalse(result);
    }

    @Test
    void GivenAndRule_WhenSecondRuleFails_ThenReturnFalse() {
        // Arrange
        IPurchaseRule rule = new AndRule(new MinTicketsRule(2), new MaxTicketsRule(5));
        PurchaseContext context = new PurchaseContext(6, 25);

        // Act
        boolean result = rule.validate(context);

        // Assert
        assertFalse(result);
    }

    // OrRule

    @Test
    void GivenOrRule_WhenFirstRulePasses_ThenReturnTrue() {
        // Arrange
        IPurchaseRule rule = new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100));
        PurchaseContext context = new PurchaseContext(2, 25);

        // Act
        boolean result = rule.validate(context);

        // Assert
        assertTrue(result);
    }

    @Test
    void GivenOrRule_WhenSecondRulePasses_ThenReturnTrue() {
        // Arrange
        IPurchaseRule rule = new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100));
        PurchaseContext context = new PurchaseContext(100, 25);

        // Act
        boolean result = rule.validate(context);

        // Assert
        assertTrue(result);
    }

    @Test
    void GivenOrRule_WhenAllRulesFail_ThenReturnFalse() {
        // Arrange
        IPurchaseRule rule = new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100));
        PurchaseContext context = new PurchaseContext(50, 25);

        // Act
        boolean result = rule.validate(context);

        // Assert
        assertFalse(result);
    }

    // Nested composite: AND(minAge, OR(maxTickets, minTickets))

    @Test
    void GivenNestedComposite_WhenAdultBuysSmallAmount_ThenReturnTrue() {
        // Arrange
        IPurchaseRule rule = new AndRule(
                new MinAgeRule(18),
                new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100)));
        PurchaseContext context = new PurchaseContext(1, 20);

        // Act
        boolean result = rule.validate(context);

        // Assert
        assertTrue(result);
    }

    @Test
    void GivenNestedComposite_WhenAdultBuysLargeAmount_ThenReturnTrue() {
        // Arrange
        IPurchaseRule rule = new AndRule(
                new MinAgeRule(18),
                new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100)));
        PurchaseContext context = new PurchaseContext(100, 20);

        // Act
        boolean result = rule.validate(context);

        // Assert
        assertTrue(result);
    }

    @Test
    void GivenNestedComposite_WhenAdultBuysMidAmount_ThenReturnFalse() {
        // Arrange
        IPurchaseRule rule = new AndRule(
                new MinAgeRule(18),
                new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100)));
        PurchaseContext context = new PurchaseContext(50, 20);

        // Act
        boolean result = rule.validate(context);

        // Assert
        assertFalse(result);
    }

    @Test
    void GivenNestedComposite_WhenUnderageUser_ThenReturnFalse() {
        // Arrange
        IPurchaseRule rule = new AndRule(
                new MinAgeRule(18),
                new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100)));
        PurchaseContext context = new PurchaseContext(1, 16);

        // Act
        boolean result = rule.validate(context);

        // Assert
        assertFalse(result);
    }

    // PurchasePolicy container

    @Test
    void GivenEmptyPolicy_WhenValidate_ThenReturnTrue() {
        // Arrange
        PurchasePolicy policy = new PurchasePolicy();
        PurchaseContext context = new PurchaseContext(10, 25);

        // Act
        boolean result = policy.validate(context);

        // Assert
        assertTrue(result);
    }

    @Test
    void GivenPolicy_WhenAllRulesPass_ThenReturnTrue() {
        // Arrange
        PurchasePolicy policy = new PurchasePolicy();
        policy.addRule(new MinAgeRule(18));
        policy.addRule(new MaxTicketsRule(5));
        PurchaseContext context = new PurchaseContext(3, 20);

        // Act
        boolean result = policy.validate(context);

        // Assert
        assertTrue(result);
    }

    @Test
    void GivenPolicy_WhenOneRuleFails_ThenReturnFalse() {
        // Arrange
        PurchasePolicy policy = new PurchasePolicy();
        policy.addRule(new MinAgeRule(18));
        policy.addRule(new MaxTicketsRule(5));
        PurchaseContext context = new PurchaseContext(3, 16);

        // Act
        boolean result = policy.validate(context);

        // Assert
        assertFalse(result);
    }

    @Test
    void GivenPolicy_WithCompositeOrRule_WhenValidate_ThenEvaluatesCorrectly() {
        // Arrange
        PurchasePolicy policy = new PurchasePolicy();
        policy.addRule(new MinAgeRule(18));
        policy.addRule(new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100)));

        // Act & Assert
        assertFalse(policy.validate(new PurchaseContext(50, 20)));
        assertTrue(policy.validate(new PurchaseContext(1, 20)));
        assertTrue(policy.validate(new PurchaseContext(100, 20)));
    }

    // Concurrency

    @Test
    void GivenCompositeRule_WhenValidatedConcurrently_ThenAllThreadsReturnTrue() throws InterruptedException {
        // Arrange
        int threadCount = 20;
        IPurchaseRule rule = new AndRule(
                new MinAgeRule(18),
                new OrRule(new MaxTicketsRule(5), new MinTicketsRule(100)));
        PurchaseContext validContext = new PurchaseContext(3, 25);
        List<Boolean> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    results.add(rule.validate(validContext));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertEquals(threadCount, results.size());
        assertTrue(results.stream().allMatch(r -> r));
    }

    @Test
    void GivenCompositeRule_WhenInvalidContextValidatedConcurrently_ThenAllThreadsReturnFalse()
            throws InterruptedException {
        // Arrange
        int threadCount = 20;
        IPurchaseRule rule = new AndRule(new MinAgeRule(18), new MaxTicketsRule(5));
        PurchaseContext invalidContext = new PurchaseContext(3, 16); // age fails
        List<Boolean> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    results.add(rule.validate(invalidContext));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertEquals(threadCount, results.size());
        assertTrue(results.stream().noneMatch(r -> r));
    }

    @Test
    void GivenPurchasePolicy_WhenConcurrentMixedContexts_ThenEachResultMatchesItsContext() throws InterruptedException {
        // Arrange
        int threadCount = 20;
        PurchasePolicy policy = new PurchasePolicy();
        policy.addRule(new MinAgeRule(18));
        policy.addRule(new MaxTicketsRule(5));
        PurchaseContext valid = new PurchaseContext(3, 20);
        PurchaseContext invalid = new PurchaseContext(3, 16);
        List<Boolean> validResults = Collections.synchronizedList(new ArrayList<>());
        List<Boolean> invalidResults = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount * 2);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount * 2);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    validResults.add(policy.validate(valid));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
            executor.submit(() -> {
                try {
                    startLatch.await();
                    invalidResults.add(policy.validate(invalid));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertEquals(threadCount, validResults.size());
        assertEquals(threadCount, invalidResults.size());
        assertTrue(validResults.stream().allMatch(r -> r));
        assertTrue(invalidResults.stream().noneMatch(r -> r));
    }
}
