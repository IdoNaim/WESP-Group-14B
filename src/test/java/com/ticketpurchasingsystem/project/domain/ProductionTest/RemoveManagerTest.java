package com.ticketpurchasingsystem.project.domain.ProductionTest;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Production.*;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import com.ticketpurchasingsystem.project.infrastructure.ProdRepo;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RemoveManagerTest {

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private IProdRepo prodRepo;
    @Mock
    private ProductionEventPublisher productionEventPublisher;

    private ProductionHandler productionHandler;
    private ProductionService productionService;

    private static final String VALID_TOKEN = "valid-token";
    private static final String INVALID_TOKEN = "bad-token";
    private static final String FOUNDER_ID = "user123";
    private static final String MANAGER_ID = "user345";
    private static final Integer COMPANY_ID = 1;

    @BeforeEach
    void setUp() {
        productionHandler = new ProductionHandler();
        productionService = new ProductionService(
                authenticationService, productionHandler, prodRepo, productionEventPublisher);
    }

    private ProductionCompany companyWithFounderAndManager() {
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Test Co", "desc", "test@co.com");
        ProductionCompany company = new ProductionCompany(dto);
        company.setCompanyId(COMPANY_ID);
        company.initFounder(FOUNDER_ID);
        company.appointManager(FOUNDER_ID, MANAGER_ID, Set.of(ManagerPermission.INVENTORY_MANAGEMENT));
        return company;
    }

    // ── Service-level tests ──────────────────────────────────────────────────

    @Test
    public void GivenInvalidToken_WhenRemoveManager_ThenReturnFalse() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        boolean result = productionService.removeManager(INVALID_TOKEN, COMPANY_ID, MANAGER_ID);

        assertFalse(result);
        verifyNoInteractions(prodRepo, productionEventPublisher);
    }

    @Test
    public void GivenCompanyNotFound_WhenRemoveManager_ThenReturnFalse() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.empty());

        boolean result = productionService.removeManager(VALID_TOKEN, COMPANY_ID, MANAGER_ID);

        assertFalse(result);
        verify(prodRepo, never()).save(any());
    }

    @Test
    public void GivenValidInput_WhenRemoveManager_ThenReturnTrue() {
        ProductionCompany company = companyWithFounderAndManager();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean result = productionService.removeManager(VALID_TOKEN, COMPANY_ID, MANAGER_ID);

        assertTrue(result);
    }

    @Test
    public void GivenCallerIsNotOwner_WhenRemoveManager_ThenReturnFalseAndRepoSaveNeverCalled() {
        ProductionCompany company = companyWithFounderAndManager();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn("not-an-owner");
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        boolean result = productionService.removeManager(VALID_TOKEN, COMPANY_ID, MANAGER_ID);

        assertFalse(result);
        verify(prodRepo, never()).save(any());
    }

    @Test
    public void GivenNonExistentManager_WhenRemoveManager_ThenReturnFalse() {
        ProductionCompany company = companyWithFounderAndManager();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        boolean result = productionService.removeManager(VALID_TOKEN, COMPANY_ID, "ghost-user");

        assertFalse(result);
        verify(prodRepo, never()).save(any());
    }

    @Test
    public void GivenGeneralSaveException_WhenRemoveManager_ThenReturnFalseWithNoRetry() {
        ProductionCompany company = companyWithFounderAndManager();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(any())).thenThrow(new RuntimeException("DB down"));

        boolean result = productionService.removeManager(VALID_TOKEN, COMPANY_ID, MANAGER_ID);

        assertFalse(result);
        verify(prodRepo, times(1)).save(any());
    }

    @Test
    public void GivenOptimisticLockingFailureOnFirstAttempt_WhenRemoveManager_ThenRetryAndReturnTrue() {
        ProductionCompany company = companyWithFounderAndManager();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(any()))
                .thenThrow(new OptimisticLockingFailureException("conflict"))
                .thenAnswer(inv -> inv.getArgument(0));

        boolean result = productionService.removeManager(VALID_TOKEN, COMPANY_ID, MANAGER_ID);

        assertTrue(result);
        verify(prodRepo, times(2)).save(any());
    }

    @Test
    public void Given10ConcurrentThreadsWithValidToken_WhenRemoveManager_ThenAtLeastOneSucceeds()
            throws InterruptedException {
        int threads = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger saveCallCount = new AtomicInteger(0);

        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);

        // Each thread gets its own company snapshot (simulates DB read per attempt)
        when(prodRepo.findById(COMPANY_ID)).thenAnswer(inv -> {
            ProductionCompany c = companyWithFounderAndManager();
            return Optional.of(c);
        });
        when(prodRepo.save(any())).thenAnswer(inv -> {
            if (saveCallCount.incrementAndGet() == 1)
                return inv.getArgument(0);
            throw new OptimisticLockingFailureException("concurrent");
        });

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (productionService.removeManager(VALID_TOKEN, COMPANY_ID, MANAGER_ID))
                        successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(successCount.get() >= 1);
    }

    // ── Handler-level tests ──────────────────────────────────────────────────

    @Test
    public void GivenValidInput_WhenHandlerRemoveManager_ThenReturnUpdatedCompany() {
        ProductionCompany company = companyWithFounderAndManager();

        ProductionCompany result = productionHandler.removeManager(
                FOUNDER_ID, COMPANY_ID, MANAGER_ID, company);

        assertNotNull(result);
    }

    @Test
    public void GivenValidInput_WhenHandlerRemoveManager_ThenManagerIsNoLongerInCompany() {
        ProductionCompany company = companyWithFounderAndManager();

        productionHandler.removeManager(FOUNDER_ID, COMPANY_ID, MANAGER_ID, company);

        assertFalse(company.isManager(MANAGER_ID));
    }

    @Test
    public void GivenCallerIsNotOwner_WhenHandlerRemoveManager_ThenReturnNull() {
        ProductionCompany company = companyWithFounderAndManager();

        ProductionCompany result = productionHandler.removeManager(
                "random-user", COMPANY_ID, MANAGER_ID, company);

        assertNull(result);
    }

    @Test
    public void GivenManagerNotAppointedByCaller_WhenHandlerRemoveManager_ThenReturnNull() {
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Test Co", "desc", "test@co.com");
        ProductionCompany company = new ProductionCompany(dto);
        company.setCompanyId(COMPANY_ID);
        company.initFounder(FOUNDER_ID);
        // MANAGER_ID is appointed by FOUNDER_ID, not by "other-owner"
        company.appointManager(FOUNDER_ID, MANAGER_ID, Set.of(ManagerPermission.INVENTORY_MANAGEMENT));

        ProductionCompany result = productionHandler.removeManager(
                "other-owner", COMPANY_ID, MANAGER_ID, company);

        assertNull(result);
        assertTrue(company.isManager(MANAGER_ID));
    }

    @Test
    public void GivenUserIsNotManagerInCompany_WhenHandlerRemoveManager_ThenReturnNull() {
        ProductionCompany company = companyWithFounderAndManager();

        ProductionCompany result = productionHandler.removeManager(
                FOUNDER_ID, COMPANY_ID, "unknown-user", company);

        assertNull(result);
    }

    @Test
    public void GivenNullManagerId_WhenHandlerRemoveManager_ThenReturnNull() {
        ProductionCompany company = companyWithFounderAndManager();

        ProductionCompany result = productionHandler.removeManager(
                FOUNDER_ID, COMPANY_ID, null, company);

        assertNull(result);
    }

    @Test
    public void GivenNullCompanyObject_WhenHandlerRemoveManager_ThenReturnNull() {
        ProductionCompany result = productionHandler.removeManager(
                FOUNDER_ID, COMPANY_ID, MANAGER_ID, null);

        assertNull(result);
    }

    // Concurrency test only one succeeds

    private static final String CONC_SECRET = "my-super-secret-key-for-testing!";

    private record ConcurrencyStack(AuthenticationService auth, ProdRepo repo, ProductionService service) {
    }

    private ConcurrencyStack buildRealStack() {
        InMemorySessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuth = new DomainAuthService(sessionRepo);
        ReflectionTestUtils.setField(domainAuth, "secret", CONC_SECRET);
        domainAuth.init();
        AuthenticationService realAuth = new AuthenticationService(domainAuth, sessionRepo);
        ProdRepo realRepo = new ProdRepo();
        ProductionService svc = new ProductionService(realAuth, new ProductionHandler(), realRepo,
                productionEventPublisher);
        return new ConcurrencyStack(realAuth, realRepo, svc);
    }

    @Test
    public void GivenMultipleThreads_WhenConcurrentRemoveSameManager_ThenOnlyOneSucceeds() throws Exception {
        // Arrange
        lenient().when(productionEventPublisher.publishIsUserRegisteredEvent(any())).thenReturn(true);
        ConcurrencyStack stack = buildRealStack();
        String founderToken = stack.auth().login(FOUNDER_ID);
        stack.service().createProductionCompany(founderToken,
                new ProductionCompanyDTO("Remove Co", "desc", "remove@co.com"));
        int companyId = stack.repo().findByName("Remove Co").get().getCompanyId();
        stack.service().appointManager(founderToken, companyId, "same-manager",
                Set.of(ManagerPermission.INVENTORY_MANAGEMENT));

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String token = stack.auth().login(FOUNDER_ID);
                    boolean result = stack.service().removeManager(token, companyId, "same-manager");
                    if (result)
                        successCount.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Act
        startLatch.countDown();
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS));
        executor.shutdown();

        // Assert
        assertEquals(1, successCount.get(),
                "Only one thread must succeed when all race to remove the same manager — subsequent attempts find no manager to remove");
    }
}
