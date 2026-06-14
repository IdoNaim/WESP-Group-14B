package com.ticketpurchasingsystem.project.acceptance.production;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;

@SpringBootTest
@ActiveProfiles("test")
class CreateProductionCompanyAcceptanceTest {

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
        ProductionEventPublisher publisher = new ProductionEventPublisher(event -> {});
        productionService = new ProductionService(authService, new ProductionHandler(), prodRepo, publisher);
    }

    @AfterEach
    void tearDown() {
        prodRepo.deleteAll();
    }

    @Test
    void GivenLoggedInUser_WhenCreateCompany_ThenReturnTrue() {
        // Arrange
        String token = authService.login("eden");
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Eden Events", "Great events", "eden@events.com");

        // Act
        Integer result = productionService.createProductionCompany(token, dto);

        // Assert
        assertNotNull(result);
    }

    @Test
    void GivenLoggedInUser_WhenCreateCompany_ThenCompanyIsPersistedWithCorrectFounder() {
        // Arrange
        String token = authService.login("eden");
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Eden Events", "desc", "eden@events.com");

        // Act
        productionService.createProductionCompany(token, dto);

        // Assert
        Optional<ProductionCompany> saved = prodRepo.findByName("Eden Events");
        assertTrue(saved.isPresent());
        assertEquals("eden", saved.get().getFounderId());
    }

    // Fail

    @Test
    void GivenNotLoggedIn_WhenCreateCompany_ThenReturnFalse() {
        // Arrange
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Eden Events", "desc", "eden@events.com");

        // Act
        Integer result = productionService.createProductionCompany("invalid-token", dto);

        // Assert
        assertNull(result);
    }

    @Test
    void GivenDuplicateCompanyName_WhenCreateCompany_ThenReturnFalse() {
        // Arrange
        String token = authService.login("eden");
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Duplicate Corp", "desc", "dup@corp.com");
        productionService.createProductionCompany(token, dto);

        // Act
        Integer secondResult = productionService.createProductionCompany(token, dto);

        // Assert
        assertNull(secondResult);
    }

    @Test
    void GivenBlankCompanyName_WhenCreateCompany_ThenReturnFalse() {
        // Arrange
        String token = authService.login("eden");
        ProductionCompanyDTO dto = new ProductionCompanyDTO("   ", "desc", "eden@events.com");

        // Act
        Integer result = productionService.createProductionCompany(token, dto);

        // Assert
        assertNull(result);
    }

    // ─── Concurrency Tests ───────────────────────────────────────────────────

    @Test
    void GivenSameCompanyName_WhenConcurrentCreate_ThenAtMostOneCompanyIsCreated() throws Exception {
        String edenToken = authService.login("eden");
        String tomerToken = authService.login("tomer");
        ProductionCompanyDTO dto = new ProductionCompanyDTO("SharedCompany", "desc", "shared@co.com");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        new Thread(() -> {
            try {
                startLatch.await();
                Integer id = productionService.createProductionCompany(edenToken, dto);
                if (id != null) successCount.incrementAndGet();
                else failureCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        }).start();

        new Thread(() -> {
            try {
                startLatch.await();
                Integer id = productionService.createProductionCompany(tomerToken, dto);
                if (id != null) successCount.incrementAndGet();
                else failureCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        }).start();

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Concurrent company creation must complete without deadlock");

        long companiesWithName = prodRepo.findByName("SharedCompany").isPresent() ? 1 : 0;
        assertEquals(1, companiesWithName, "At most one company with the same name must exist");
        assertEquals(1, successCount.get() + failureCount.get() == 2 ? successCount.get() : successCount.get(),
                "Exactly one creation attempt must be reflected in the store");
    }

    @Test
    void GivenDifferentCompanyNames_WhenConcurrentCreate_ThenBothCompaniesAreCreated() throws Exception {
        String itayToken = authService.login("itay");
        String edenToken = authService.login("eden");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        CopyOnWriteArrayList<Integer> createdIds = new CopyOnWriteArrayList<>();
        AtomicInteger errorCount = new AtomicInteger(0);

        new Thread(() -> {
            try {
                startLatch.await();
                Integer id = productionService.createProductionCompany(
                        itayToken, new ProductionCompanyDTO("Itay Events", "desc", "itay@events.com"));
                if (id != null) createdIds.add(id);
            } catch (Exception e) {
                errorCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        }).start();

        new Thread(() -> {
            try {
                startLatch.await();
                Integer id = productionService.createProductionCompany(
                        edenToken, new ProductionCompanyDTO("Eden Events", "desc", "eden@events.com"));
                if (id != null) createdIds.add(id);
            } catch (Exception e) {
                errorCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        }).start();

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Concurrent company creation must complete without deadlock");

        assertEquals(0, errorCount.get(), "No exceptions should occur when creating companies with different names");
        assertEquals(2, createdIds.size(), "Both companies must be created successfully");
        assertTrue(prodRepo.findByName("Itay Events").isPresent(), "Itay Events must be in the repo");
        assertTrue(prodRepo.findByName("Eden Events").isPresent(), "Eden Events must be in the repo");
    }

    @Test
    void GivenInvalidToken_WhenConcurrentCreateWithValidToken_ThenOnlyValidTokenSucceeds() throws Exception {
        String itayToken = authService.login("itay");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        new Thread(() -> {
            try {
                startLatch.await();
                Integer id = productionService.createProductionCompany(
                        itayToken, new ProductionCompanyDTO("Itay Corp", "desc", "itay@corp.com"));
                if (id != null) successCount.incrementAndGet();
                else failureCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        }).start();

        new Thread(() -> {
            try {
                startLatch.await();
                Integer id = productionService.createProductionCompany(
                        "invalid-token-tomer", new ProductionCompanyDTO("Tomer Corp", "desc", "tomer@corp.com"));
                if (id != null) successCount.incrementAndGet();
                else failureCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        }).start();

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Concurrent create must complete without deadlock");

        assertEquals(1, successCount.get(), "Only the valid-token thread should create a company");
        assertEquals(1, failureCount.get(), "The invalid-token thread must be rejected");
        assertTrue(prodRepo.findByName("Itay Corp").isPresent(), "Itay Corp must be created");
        assertTrue(prodRepo.findByName("Tomer Corp").isEmpty(), "Tomer Corp must not be created");
    }
}
