package com.ticketpurchasingsystem.project.acceptance.production;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.IsUserRegisteredEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AssignOwnerAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";
    private static final String FOUNDER = "founder-eden";
    private static final String NEW_OWNER = "new-owner-tomer";

    private final Set<String> registeredUsers = new HashSet<>();

    @Autowired
    private IProdRepo prodRepo;

    @Autowired
    private ISessionRepo sessionRepo;

    private AuthenticationService authService;
    private ProductionService productionService;
    private int companyId;

    @BeforeEach
    void setUp() {
        registeredUsers.clear();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        ReflectionTestUtils.setField(domainAuthService, "secret", TEST_SECRET);
        domainAuthService.init();
        authService = new AuthenticationService(domainAuthService, sessionRepo);
        ProductionEventPublisher publisher = new ProductionEventPublisher(event -> {
            if (event instanceof IsUserRegisteredEvent e) {
                e.setRegistered(registeredUsers.contains(e.getUserId()));
            }
        });
        productionService = new ProductionService(authService, new ProductionHandler(), prodRepo, publisher);

        String founderToken = authService.login(FOUNDER);
        productionService.createProductionCompany(founderToken,
                new ProductionCompanyDTO("Events Co", "desc", "events@co.com"));
        companyId = prodRepo.findByName("Events Co").get().getCompanyId();
    }

    @Test
    void GivenFounderAssignsOwner_WhenAssignOwner_ThenReturnTrue() {
        // Arrange
        registeredUsers.add(NEW_OWNER);
        String founderToken = authService.login(FOUNDER);

        // Act
        boolean result = productionService.assignOwner(founderToken, companyId, NEW_OWNER);

        // Assert
        assertTrue(result);
    }

    @Test
    void GivenFounderAssignsOwner_WhenAssignOwner_ThenNewOwnerAppearsInCompany() {
        // Arrange
        registeredUsers.add(NEW_OWNER);
        String founderToken = authService.login(FOUNDER);

        // Act
        productionService.assignOwner(founderToken, companyId, NEW_OWNER);

        // Assert
        Optional<ProductionCompany> company = prodRepo.findByName("Events Co");
        assertTrue(company.isPresent());
        assertTrue(company.get().isOwner(NEW_OWNER));
    }

    // Fail

    @Test
    void GivenInvalidToken_WhenAssignOwner_ThenReturnFalse() {
        // Arrange
        registeredUsers.add(NEW_OWNER);

        // Act
        boolean result = productionService.assignOwner("invalid-token", companyId, NEW_OWNER);

        // Assert
        assertFalse(result);
    }

    @Test
    void GivenNonOwnerAttemptsAssignOwner_WhenAssignOwner_ThenReturnFalse() {
        // Arrange
        registeredUsers.add(NEW_OWNER);
        String nonOwnerToken = authService.login("random-user");

        // Act
        boolean result = productionService.assignOwner(nonOwnerToken, companyId, NEW_OWNER);

        // Assert
        assertFalse(result);
    }

    @Test
    void GivenAlreadyOwner_WhenAssignOwner_ThenReturnFalse() {
        // Arrange
        registeredUsers.add(FOUNDER);
        String founderToken = authService.login(FOUNDER);

        // Act
        boolean result = productionService.assignOwner(founderToken, companyId, FOUNDER);

        // Assert
        assertFalse(result);
    }

    @Test
    void GivenUnregisteredUser_WhenAssignOwner_ThenReturnFalse() {
        // Arrange
        String founderToken = authService.login(FOUNDER);

        // Act
        boolean result = productionService.assignOwner(founderToken, companyId, "unregistered-user");

        // Assert
        assertFalse(result);
    }

    // ─── Concurrency Tests ───────────────────────────────────────────────────

    @Test
    void GivenTwoRegisteredUsers_WhenConcurrentAssignOwner_ThenBothSucceedWithOptimisticRetry() throws Exception {
        registeredUsers.add("itay");
        registeredUsers.add("tomer");
        String founderToken1 = authService.login(FOUNDER);
        String founderToken2 = authService.login(FOUNDER);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        new Thread(() -> {
            try {
                startLatch.await();
                boolean result = productionService.assignOwner(founderToken1, companyId, "itay");
                if (result) successCount.incrementAndGet();
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
                boolean result = productionService.assignOwner(founderToken2, companyId, "tomer");
                if (result) successCount.incrementAndGet();
                else failureCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        }).start();

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Concurrent assignOwner must complete without deadlock");

        // Optimistic locking with retry ensures both eventually succeed
        assertEquals(2, successCount.get(), "Both owner assignments must succeed via optimistic-locking retry");
        Optional<ProductionCompany> company = prodRepo.findByName("Events Co");
        assertTrue(company.isPresent());
        assertTrue(company.get().isOwner("itay"), "itay must be an owner");
        assertTrue(company.get().isOwner("tomer"), "tomer must be an owner");
    }

    @Test
    void GivenNonOwnerAndOwner_WhenConcurrentAssignOwner_ThenOnlyOwnerSucceeds() throws Exception {
        registeredUsers.add("eden");
        String founderToken = authService.login(FOUNDER);
        String nonOwnerToken = authService.login("random-user");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Thread 1: valid founder assigns "eden" — must succeed
        new Thread(() -> {
            try {
                startLatch.await();
                boolean result = productionService.assignOwner(founderToken, companyId, "eden");
                if (result) successCount.incrementAndGet();
                else failureCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        }).start();

        // Thread 2: non-owner tries to assign "eden" — must fail
        new Thread(() -> {
            try {
                startLatch.await();
                boolean result = productionService.assignOwner(nonOwnerToken, companyId, "eden");
                if (result) successCount.incrementAndGet();
                else failureCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        }).start();

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Concurrent assignOwner must complete without deadlock");

        assertEquals(1, successCount.get(), "Only the founder should successfully assign an owner");
        assertEquals(1, failureCount.get(), "The non-owner attempt must fail");
    }
}
