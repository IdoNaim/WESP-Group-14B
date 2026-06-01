package com.ticketpurchasingsystem.project.domain.ProductionTest;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import static com.ticketpurchasingsystem.project.domain.Production.ManagerPermission.none;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import com.ticketpurchasingsystem.project.domain.Production.OptimisticLockingFailureException;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import com.ticketpurchasingsystem.project.infrastructure.ProdRepo;

@ExtendWith(MockitoExtension.class)
public class ModifyManagerPermissionsTest {

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private IProdRepo prodRepo;
    @Mock
    private ProductionEventPublisher productionEventPublisher;
    @Captor
    private ArgumentCaptor<ProductionCompany> captor;

    private ProductionHandler productionHandler;
    private ProductionService productionService;

    private static final String VALID_TOKEN = "valid-token";
    private static final String INVALID_TOKEN = "bad-token";
    private static final String FOUNDER_ID = "eden-1";
    private static final String MANAGER_ID = "itay-1";
    private static final String OTHER_MANAGER_ID = "tomer-2";
    private static final Integer COMPANY_ID = 1;

    private static final Set<ManagerPermission> SOME_PERMISSIONS = EnumSet.of(
            ManagerPermission.INVENTORY_MANAGEMENT,
            ManagerPermission.PURCHASE_AND_ORDER_HISTORY_ACCESS);

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
        company.appointOwner(FOUNDER_ID, MANAGER_ID);
        return company;
    }

    // ── Service-level tests ──────────────────────────────────────────────────

    @Test
    public void GivenInvalidToken_WhenModifyManagerPermissions_ThenReturnFalse() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        boolean result = productionService.modifyManagerPermissions(
                INVALID_TOKEN, COMPANY_ID, MANAGER_ID, SOME_PERMISSIONS);

        assertFalse(result);
        verifyNoInteractions(prodRepo, productionEventPublisher);
    }

    @Test
    public void GivenCompanyNotFound_WhenModifyManagerPermissions_ThenReturnFalse() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.empty());

        boolean result = productionService.modifyManagerPermissions(
                VALID_TOKEN, COMPANY_ID, MANAGER_ID, SOME_PERMISSIONS);

        assertFalse(result);
        verify(prodRepo, never()).save(any());
    }

    @Test
    public void GivenValidInput_WhenModifyManagerPermissions_ThenReturnTrue() {
        ProductionCompany company = companyWithFounderAndManager();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean result = productionService.modifyManagerPermissions(
                VALID_TOKEN, COMPANY_ID, MANAGER_ID, SOME_PERMISSIONS);

        assertTrue(result);
    }

    @Test
    public void GivenValidInput_WhenModifyManagerPermissions_ThenRepoSaveCalledWithUpdatedCompany() {
        ProductionCompany company = companyWithFounderAndManager();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        productionService.modifyManagerPermissions(VALID_TOKEN, COMPANY_ID, MANAGER_ID, SOME_PERMISSIONS);

        verify(prodRepo, times(1)).save(any());
        assertTrue(captor.getValue().getManagerPermissions(MANAGER_ID).containsAll(SOME_PERMISSIONS));
    }

    @Test
    public void GivenCallerNotOwner_WhenModifyManagerPermissions_ThenRepoSaveNeverCalled() {
        ProductionCompany company = companyWithFounderAndManager();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn("not-an-owner");
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        boolean result = productionService.modifyManagerPermissions(
                VALID_TOKEN, COMPANY_ID, MANAGER_ID, SOME_PERMISSIONS);

        assertFalse(result);
        verify(prodRepo, never()).save(any());
    }

    @Test
    public void GivenValidInput_WhenModifyManagerPermissions_ThenEventIsPublished() {
        ProductionCompany company = companyWithFounderAndManager();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        productionService.modifyManagerPermissions(VALID_TOKEN, COMPANY_ID, MANAGER_ID, SOME_PERMISSIONS);

        verify(productionEventPublisher, times(1))
                .publishModifyManagerPermissionsEvent(any(), eq(FOUNDER_ID), eq(MANAGER_ID), eq(SOME_PERMISSIONS));
    }

    @Test
    public void GivenFailure_WhenModifyManagerPermissions_ThenEventIsNotPublished() {
        ProductionCompany company = companyWithFounderAndManager();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn("not-an-owner");
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        productionService.modifyManagerPermissions(VALID_TOKEN, COMPANY_ID, MANAGER_ID, SOME_PERMISSIONS);

        verifyNoInteractions(productionEventPublisher);
    }

    // ── Handler-level tests ──────────────────────────────────────────────────

    @Test
    public void GivenCallerNotOwner_WhenHandlerModifyPermissions_ThenReturnNull() {
        ProductionCompany company = companyWithFounderAndManager();

        ProductionCompany result = productionHandler.modifyManagerPermissions(
                "random-user", COMPANY_ID, MANAGER_ID, SOME_PERMISSIONS, company);

        assertNull(result);
    }

    @Test
    public void GivenManagerNotInCompany_WhenHandlerModifyPermissions_ThenReturnNull() {
        ProductionCompany company = companyWithFounderAndManager();

        ProductionCompany result = productionHandler.modifyManagerPermissions(
                FOUNDER_ID, COMPANY_ID, "unknown-user", SOME_PERMISSIONS, company);

        assertNull(result);
    }

    @Test
    public void GivenManagerNotAppointedByCaller_WhenHandlerModifyPermissions_ThenReturnNull() {
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Test Co", "desc", "test@co.com");
        ProductionCompany company = new ProductionCompany(dto);
        company.setCompanyId(COMPANY_ID);
        company.initFounder(FOUNDER_ID);
        company.appointOwner(FOUNDER_ID, MANAGER_ID);
        company.appointOwner(FOUNDER_ID, OTHER_MANAGER_ID);

        // MANAGER_ID tries to modify OTHER_MANAGER_ID's permissions, but didn't appoint them
        ProductionCompany result = productionHandler.modifyManagerPermissions(
                MANAGER_ID, COMPANY_ID, OTHER_MANAGER_ID, SOME_PERMISSIONS, company);

        assertNull(result);
    }

    @Test
    public void GivenValidInput_WhenHandlerModifyPermissions_ThenReturnCompany() {
        ProductionCompany company = companyWithFounderAndManager();

        ProductionCompany result = productionHandler.modifyManagerPermissions(
                FOUNDER_ID, COMPANY_ID, MANAGER_ID, SOME_PERMISSIONS, company);

        assertNotNull(result);
    }

    @Test
    public void GivenValidInput_WhenHandlerModifyPermissions_ThenPermissionsAreStored() {
        ProductionCompany company = companyWithFounderAndManager();

        productionHandler.modifyManagerPermissions(
                FOUNDER_ID, COMPANY_ID, MANAGER_ID, SOME_PERMISSIONS, company);

        assertEquals(SOME_PERMISSIONS, company.getManagerPermissions(MANAGER_ID));
    }

    @Test
    public void GivenNullManagerId_WhenHandlerModifyPermissions_ThenReturnNull() {
        ProductionCompany company = companyWithFounderAndManager();

        ProductionCompany result = productionHandler.modifyManagerPermissions(
                FOUNDER_ID, COMPANY_ID, null, SOME_PERMISSIONS, company);

        assertNull(result);
    }

    @Test
    public void GivenNullPermissions_WhenHandlerModifyPermissions_ThenReturnNull() {
        ProductionCompany company = companyWithFounderAndManager();

        ProductionCompany result = productionHandler.modifyManagerPermissions(
                FOUNDER_ID, COMPANY_ID, MANAGER_ID, null, company);

        assertNull(result);
    }

    @Test
    public void GivenEmptyPermissions_WhenHandlerModifyPermissions_ThenPermissionsAreCleared() {
        ProductionCompany company = companyWithFounderAndManager();
        company.setManagerPermissions(MANAGER_ID, SOME_PERMISSIONS);

        productionHandler.modifyManagerPermissions(
                FOUNDER_ID, COMPANY_ID, MANAGER_ID, none(), company);

        assertTrue(company.getManagerPermissions(MANAGER_ID).isEmpty());
    }

    @Test
    public void GivenOwnerAppointed_WhenOwnerModifiesTheirManagerPermissions_ThenReturnCompany() {
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Test Co", "desc", "test@co.com");
        ProductionCompany company = new ProductionCompany(dto);
        company.setCompanyId(COMPANY_ID);
        company.initFounder(FOUNDER_ID);
        company.appointOwner(FOUNDER_ID, MANAGER_ID);
        company.appointOwner(MANAGER_ID, OTHER_MANAGER_ID);

        ProductionCompany result = productionHandler.modifyManagerPermissions(
                MANAGER_ID, COMPANY_ID, OTHER_MANAGER_ID, SOME_PERMISSIONS, company);

        assertNotNull(result);
        assertEquals(SOME_PERMISSIONS, result.getManagerPermissions(OTHER_MANAGER_ID));
    }

    // ── Concurrency tests ────────────────────────────────────────────────────

    private static final String CONC_SECRET = "my-super-secret-key-for-testing!";

    private record ConcurrencyStack(AuthenticationService auth, ProdRepo repo, ProductionService service) {}

    private ConcurrencyStack buildRealStack() {
        InMemorySessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuth = new DomainAuthService(sessionRepo);
        ReflectionTestUtils.setField(domainAuth, "secret", CONC_SECRET);
        domainAuth.init();
        AuthenticationService realAuth = new AuthenticationService(domainAuth, sessionRepo);
        ProdRepo realRepo = new ProdRepo();
        ProductionService svc = new ProductionService(realAuth, new ProductionHandler(), realRepo, productionEventPublisher);
        return new ConcurrencyStack(realAuth, realRepo, svc);
    }

    @Test
    public void GivenMultipleThreads_WhenConcurrentModifyDifferentOwnerPermissions_ThenAllSucceedThroughRetries()
            throws Exception {
        // Arrange
        lenient().when(productionEventPublisher.publishIsUserRegisteredEvent(any())).thenReturn(true);
        ConcurrencyStack stack = buildRealStack();
        String founderToken = stack.auth().login(FOUNDER_ID);
        stack.service().createProductionCompany(founderToken,
                new ProductionCompanyDTO("Perm Co", "desc", "perm@co.com"));
        int companyId = stack.repo().findByName("Perm Co").get().getCompanyId();

        int threadCount = 3;
        for (int i = 0; i < threadCount; i++) {
            stack.service().assignOwner(founderToken, companyId, "target-owner-" + i);
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String token = stack.auth().login(FOUNDER_ID);
                    boolean result = stack.service().modifyManagerPermissions(
                            token, companyId, "target-owner-" + idx, SOME_PERMISSIONS);
                    if (result) successCount.incrementAndGet();
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
        assertEquals(threadCount, successCount.get(),
                "All concurrent modify-permissions operations on different owners must succeed through optimistic-locking retries");
    }

    @Test
    public void GivenMultipleThreads_WhenConcurrentModifySameOwnerPermissions_ThenOnlyOneSucceeds()
            throws Exception {
        // Arrange
        int threadCount = 5;
        AtomicInteger saveCallCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenAnswer(inv -> Optional.of(companyWithFounderAndManager()));
        when(prodRepo.save(any())).thenAnswer(inv -> {
            if (saveCallCount.incrementAndGet() == 1) return inv.getArgument(0);
            throw new OptimisticLockingFailureException("concurrent");
        });

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (productionService.modifyManagerPermissions(VALID_TOKEN, COMPANY_ID, MANAGER_ID, SOME_PERMISSIONS))
                        successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Act
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertEquals(1, successCount.get(),
                "Only one thread must succeed when all race to modify the same owner's permissions — others must exhaust retries");
    }
}
