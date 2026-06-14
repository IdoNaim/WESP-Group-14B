package com.ticketpurchasingsystem.project.acceptance.production;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;


import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.PurchaseContext;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.AndRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.MaxTicketsRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.MinAgeRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.MinTicketsRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.OrRule;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;

@SpringBootTest
@ActiveProfiles("test")
class ProductionPurchasePolicyAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";

    @Autowired
    private IProdRepo prodRepo;

    @Autowired
    private ISessionRepo sessionRepo;

    private AuthenticationService authService;
    private ProductionService productionService;

    @BeforeEach
    void setUp() {
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        ReflectionTestUtils.setField(domainAuthService, "secret", TEST_SECRET);
        domainAuthService.init();
        authService = new AuthenticationService(domainAuthService, sessionRepo);
        ProductionEventPublisher publisher = new ProductionEventPublisher(event -> {
        });
        productionService = new ProductionService(authService, new ProductionHandler(), prodRepo, publisher);
    }

    @AfterEach
    void tearDown() {
        prodRepo.deleteAll();
    }

    private Integer createCompany(String token, String name) {
        productionService.createProductionCompany(token, new ProductionCompanyDTO(name, "desc", "test@company.com"));
        return prodRepo.findByName(name).get().getCompanyId();
    }

    // owner adds leaf rules

    @Test
    void GivenOwner_WhenAddMinAgeRule_ThenPolicyBlocksUnderage() {
        // Arrange
        String token = authService.login("owner");
        Integer companyId = createCompany(token, "AgePolicy Corp");

        // Act
        boolean added = productionService.addPurchasePolicyRule(token, companyId, new MinAgeRule(18));

        // Assert
        assertTrue(added);
        ProductionCompany company = prodRepo.findById(companyId).get();
        assertFalse(company.getPurchasePolicy().validate(new PurchaseContext(1, 17)));
        assertTrue(company.getPurchasePolicy().validate(new PurchaseContext(1, 18)));
    }

    @Test
    void GivenOwner_WhenAddMaxTicketsRule_ThenPolicyEnforcesLimit() {
        // Arrange
        String token = authService.login("owner");
        Integer companyId = createCompany(token, "Tickets Corp");

        // Act
        boolean added = productionService.addPurchasePolicyRule(token, companyId, new MaxTicketsRule(5));

        // Assert
        assertTrue(added);
        ProductionCompany company = prodRepo.findById(companyId).get();
        assertFalse(company.getPurchasePolicy().validate(new PurchaseContext(6, 25)));
        assertTrue(company.getPurchasePolicy().validate(new PurchaseContext(5, 25)));
    }

    // owner adds composite rules

    @Test
    void GivenOwner_WhenAddAndRule_ThenPolicyEnforcesAllConditions() {
        // Arrange
        String token = authService.login("owner");
        Integer companyId = createCompany(token, "AndRule Corp");
        IPurchaseRule rule = new AndRule(new MinTicketsRule(2), new MaxTicketsRule(5));

        // Act
        boolean added = productionService.addPurchasePolicyRule(token, companyId, rule);

        // Assert
        assertTrue(added);
        ProductionCompany company = prodRepo.findById(companyId).get();
        assertFalse(company.getPurchasePolicy().validate(new PurchaseContext(1, 25))); // below min
        assertFalse(company.getPurchasePolicy().validate(new PurchaseContext(6, 25))); // above max
        assertTrue(company.getPurchasePolicy().validate(new PurchaseContext(3, 25))); // in range
    }

    @Test
    void GivenOwner_WhenAddOrRule_ThenPolicyAllowsEitherCondition() {
        // Arrange
        String token = authService.login("owner");
        Integer companyId = createCompany(token, "OrRule Corp");
        IPurchaseRule rule = new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100));

        // Act
        boolean added = productionService.addPurchasePolicyRule(token, companyId, rule);

        // Assert
        assertTrue(added);
        ProductionCompany company = prodRepo.findById(companyId).get();
        assertTrue(company.getPurchasePolicy().validate(new PurchaseContext(1, 25))); // <= 2
        assertTrue(company.getPurchasePolicy().validate(new PurchaseContext(100, 25))); // >= 100
        assertFalse(company.getPurchasePolicy().validate(new PurchaseContext(50, 25))); // neither
    }

    @Test
    void GivenOwner_WhenAddNestedCompositeRule_ThenPolicyEvaluatesDepthCorrectly() {
        // Arrange
        String token = authService.login("owner");
        Integer companyId = createCompany(token, "Nested Corp");
        // "From age 18 AND (up to 2 tickets OR at least 100 tickets)"
        IPurchaseRule rule = new AndRule(
                new MinAgeRule(18),
                new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100)));

        // Act
        boolean added = productionService.addPurchasePolicyRule(token, companyId, rule);

        // Assert
        assertTrue(added);
        ProductionCompany company = prodRepo.findById(companyId).get();
        assertTrue(company.getPurchasePolicy().validate(new PurchaseContext(1, 20))); // adult, <=2
        assertTrue(company.getPurchasePolicy().validate(new PurchaseContext(100, 20))); // adult, >=100
        assertFalse(company.getPurchasePolicy().validate(new PurchaseContext(50, 20))); // adult, mid amount
        assertFalse(company.getPurchasePolicy().validate(new PurchaseContext(1, 16))); // underage
    }

    @Test
    void GivenOwner_WhenAddMultipleRulesSequentially_ThenAllRulesAreEnforced() {
        // Arrange
        String token = authService.login("owner");
        Integer companyId = createCompany(token, "Multi Corp");

        // Act
        productionService.addPurchasePolicyRule(token, companyId, new MinAgeRule(18));
        productionService.addPurchasePolicyRule(token, companyId, new MaxTicketsRule(5));

        // Assert
        ProductionCompany company = prodRepo.findById(companyId).get();
        assertTrue(company.getPurchasePolicy().validate(new PurchaseContext(3, 20))); // both pass
        assertFalse(company.getPurchasePolicy().validate(new PurchaseContext(3, 16))); // age fails
        assertFalse(company.getPurchasePolicy().validate(new PurchaseContext(6, 20))); // tickets fails
    }

    // fail-unauthorized users

    @Test
    void GivenNonOwner_WhenAddPurchasePolicyRule_ThenReturnFalse() {
        // Arrange
        String ownerToken = authService.login("owner");
        Integer companyId = createCompany(ownerToken, "Security Corp");
        String nonOwnerToken = authService.login("nonOwner");

        // Act
        boolean result = productionService.addPurchasePolicyRule(nonOwnerToken, companyId, new MinAgeRule(18));

        // Assert
        assertFalse(result);
    }

    @Test
    void GivenInvalidToken_WhenAddPurchasePolicyRule_ThenReturnFalse() {
        // Arrange
        String ownerToken = authService.login("owner");
        Integer companyId = createCompany(ownerToken, "Token Corp");

        // Act
        boolean result = productionService.addPurchasePolicyRule("invalid-token", companyId, new MinAgeRule(18));

        // Assert
        assertFalse(result);
    }

    @Test
    void GivenOwner_WhenAddRuleToNonExistentCompany_ThenReturnFalse() {
        // Arrange
        String token = authService.login("owner");

        // Act
        boolean result = productionService.addPurchasePolicyRule(token, 99999, new MinAgeRule(18));

        // Assert
        assertFalse(result);
    }

    // concurrency tests

    @Test
    void GivenTwoThreads_WhenBothAddRulesConcurrently_ThenAtLeastOneSucceeds() throws InterruptedException {
        // Arrange
        String token = authService.login("owner");
        Integer companyId = createCompany(token, "Concurrent Corp");
        List<Boolean> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Act
        executor.submit(() -> {
            try {
                startLatch.await();
                results.add(productionService.addPurchasePolicyRule(token, companyId, new MinAgeRule(18)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                startLatch.await();
                results.add(productionService.addPurchasePolicyRule(token, companyId, new MaxTicketsRule(5)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });
        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(r -> r)); // at least one succeeds
    }

    @Test
    void GivenPolicySet_WhenValidatedConcurrently_ThenAllThreadsGetConsistentResult() throws InterruptedException {
        // Arrange
        int threadCount = 20;
        String token = authService.login("owner");
        Integer companyId = createCompany(token, "ValidateConcurrent Corp");
        productionService.addPurchasePolicyRule(token, companyId, new MinAgeRule(18));
        productionService.addPurchasePolicyRule(token, companyId, new MaxTicketsRule(5));
        ProductionCompany company = prodRepo.findById(companyId).get();
        PurchaseContext validContext = new PurchaseContext(3, 20);
        PurchaseContext invalidContext = new PurchaseContext(3, 16);
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
                    validResults.add(company.getPurchasePolicy().validate(validContext));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
            executor.submit(() -> {
                try {
                    startLatch.await();
                    invalidResults.add(company.getPurchasePolicy().validate(invalidContext));
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

    @Test
    void GivenMultipleNonOwners_WhenAllTryToAddRuleConcurrently_ThenAllFail() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        String ownerToken = authService.login("owner");
        Integer companyId = createCompany(ownerToken, "MultiNonOwner Corp");
        List<Boolean> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Act
        for (int i = 0; i < threadCount; i++) {
            String nonOwnerToken = authService.login("nonOwner" + i);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    results.add(productionService.addPurchasePolicyRule(nonOwnerToken, companyId, new MinAgeRule(18)));
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
}
