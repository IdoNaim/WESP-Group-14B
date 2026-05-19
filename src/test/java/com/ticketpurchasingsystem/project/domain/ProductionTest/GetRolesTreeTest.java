package com.ticketpurchasingsystem.project.domain.ProductionTest;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Production.*;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.RolesTreeDTO;
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
public class GetRolesTreeTest {

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private IProdRepo prodRepo;
    @Mock
    private ProductionEventPublisher productionEventPublisher;

    private ProductionHandler productionHandler;
    private ProductionService productionService;

    private static final String VALID_TOKEN   = "valid-token";
    private static final String INVALID_TOKEN = "bad-token";
    private static final String FOUNDER_ID    = "user123";
    private static final String OWNER_ID      = "user456";
    private static final String MANAGER_ID    = "user789";
    private static final Integer COMPANY_ID   = 1;

    @BeforeEach
    void setUp() {
        productionHandler = new ProductionHandler();
        productionService = new ProductionService(
                authenticationService, productionHandler, prodRepo, productionEventPublisher);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ProductionCompany companyWithFounderAndOwnerAndManager() {
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Test Co", "desc", "test@co.com");
        ProductionCompany company = new ProductionCompany(dto);
        company.setCompanyId(COMPANY_ID);
        company.initFounder(FOUNDER_ID);
        company.appointOwner(FOUNDER_ID, OWNER_ID);
        company.appointManager(OWNER_ID, MANAGER_ID, Set.of(ManagerPermission.INVENTORY_MANAGEMENT));
        company.setManagerPermissions(MANAGER_ID, Set.of(ManagerPermission.INVENTORY_MANAGEMENT));
        return company;
    }

    private ProductionCompany companyWithFounderAndOwnerOnly() {
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Test Co", "desc", "test@co.com");
        ProductionCompany company = new ProductionCompany(dto);
        company.setCompanyId(COMPANY_ID);
        company.initFounder(FOUNDER_ID);
        company.appointOwner(FOUNDER_ID, OWNER_ID);
        return company;
    }

    // ── Service-level positive tests ─────────────────────────────────────────

    @Test
    public void SuccessfulTreeLoad() {
        // Setup: users 123, 456, 789
        // 123 is founder, appoints 456 as owner
        // 456 appoints 789 as manager with full permissions
        // 123 requests the tree
        ProductionCompany company = companyWithFounderAndOwnerAndManager();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        RolesTreeDTO result = productionService.getRolesTree(VALID_TOKEN, COMPANY_ID);

        assertNotNull(result);
        assertEquals(COMPANY_ID, result.getCompanyId());
        assertEquals(FOUNDER_ID, result.getFounderId());
        assertTrue(result.getOwnershipTree().containsKey(FOUNDER_ID));
        assertTrue(result.getOwnershipTree().containsKey(OWNER_ID));
        assertTrue(result.getManagerTree().containsKey(MANAGER_ID));
        assertTrue(result.getManagerPermissions().get(MANAGER_ID)
                .contains(ManagerPermission.INVENTORY_MANAGEMENT));
    }

    @Test
    public void TreeWithNoManagers() {
        // Setup: users 123, 456
        // 123 is founder, appoints 456 as owner
        // 123 requests the tree — should show only owners, no managers
        ProductionCompany company = companyWithFounderAndOwnerOnly();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        RolesTreeDTO result = productionService.getRolesTree(VALID_TOKEN, COMPANY_ID);

        assertNotNull(result);
        assertTrue(result.getOwnershipTree().containsKey(FOUNDER_ID));
        assertTrue(result.getOwnershipTree().containsKey(OWNER_ID));
        assertTrue(result.getManagerTree().isEmpty());
        assertTrue(result.getManagerPermissions().isEmpty());
    }

    @Test
    public void GivenOwner_WhenGetRolesTree_ThenReturnTree() {
        // An owner (not founder) should also be able to fetch the tree
        ProductionCompany company = companyWithFounderAndOwnerAndManager();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(OWNER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        RolesTreeDTO result = productionService.getRolesTree(VALID_TOKEN, COMPANY_ID);

        assertNotNull(result);
    }

    @Test
    public void GivenCompanyWithNoManagers_WhenGetRolesTree_ThenManagerPermissionsIsEmpty() {
        ProductionCompany company = companyWithFounderAndOwnerOnly();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        RolesTreeDTO result = productionService.getRolesTree(VALID_TOKEN, COMPANY_ID);

        assertNotNull(result);
        assertTrue(result.getManagerPermissions().isEmpty());
    }

    // ── Service-level negative tests ─────────────────────────────────────────

    @Test
    public void GivenInvalidToken_WhenGetRolesTree_ThenReturnNull() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        RolesTreeDTO result = productionService.getRolesTree(INVALID_TOKEN, COMPANY_ID);

        assertNull(result);
        verifyNoInteractions(prodRepo);
    }

    @Test
    public void GivenCompanyNotFound_WhenGetRolesTree_ThenReturnNull() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.empty());

        RolesTreeDTO result = productionService.getRolesTree(VALID_TOKEN, COMPANY_ID);

        assertNull(result);
    }

    @Test
    public void GivenCallerIsManager_WhenGetRolesTree_ThenReturnNull() {
        // Managers are not authorized to fetch the roles tree
        ProductionCompany company = companyWithFounderAndOwnerAndManager();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(MANAGER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        RolesTreeDTO result = productionService.getRolesTree(VALID_TOKEN, COMPANY_ID);

        assertNull(result);
    }

    @Test
    public void GivenCallerIsUnrelated_WhenGetRolesTree_ThenReturnNull() {
        ProductionCompany company = companyWithFounderAndOwnerAndManager();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn("random-user");
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        RolesTreeDTO result = productionService.getRolesTree(VALID_TOKEN, COMPANY_ID);

        assertNull(result);
    }

    @Test
    public void GivenNullCompanyId_WhenGetRolesTree_ThenReturnNull() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(null)).thenReturn(Optional.empty());

        RolesTreeDTO result = productionService.getRolesTree(VALID_TOKEN, null);

        assertNull(result);
    }

    // ── Handler-level positive tests ─────────────────────────────────────────

    @Test
    public void GivenFounder_WhenHandlerGetRolesTree_ThenReturnRolesTreeDTO() {
        ProductionCompany company = companyWithFounderAndOwnerAndManager();

        RolesTreeDTO result = productionHandler.getRolesTree(FOUNDER_ID, company);

        assertNotNull(result);
        assertEquals(FOUNDER_ID, result.getFounderId());
        assertTrue(result.getOwnershipTree().containsKey(FOUNDER_ID));
        assertTrue(result.getOwnershipTree().containsKey(OWNER_ID));
        assertTrue(result.getManagerTree().containsKey(MANAGER_ID));
    }

    @Test
    public void GivenOwner_WhenHandlerGetRolesTree_ThenReturnRolesTreeDTO() {
        ProductionCompany company = companyWithFounderAndOwnerAndManager();

        RolesTreeDTO result = productionHandler.getRolesTree(OWNER_ID, company);

        assertNotNull(result);
    }

    @Test
    public void GivenManager_WhenHandlerGetRolesTree_ThenPermissionsArePresentInResult() {
        ProductionCompany company = companyWithFounderAndOwnerAndManager();

        RolesTreeDTO result = productionHandler.getRolesTree(FOUNDER_ID, company);

        assertNotNull(result);
        assertTrue(result.getManagerPermissions().containsKey(MANAGER_ID));
        assertTrue(result.getManagerPermissions().get(MANAGER_ID)
                .contains(ManagerPermission.INVENTORY_MANAGEMENT));
    }

    @Test
    public void GivenOwnerAppointed456_WhenHandlerGetRolesTree_ThenAppointedListReflectsCorrectly() {
        // FOUNDER appointed OWNER — verify via ownership tree appointer field
        ProductionCompany company = companyWithFounderAndOwnerAndManager();

        RolesTreeDTO result = productionHandler.getRolesTree(FOUNDER_ID, company);

        assertNotNull(result);
        assertEquals(FOUNDER_ID, result.getOwnershipTree().get(OWNER_ID).getAppointerId());
    }

    // ── Handler-level negative tests ─────────────────────────────────────────

    @Test
    public void GivenCallerIsNotOwnerOrFounder_WhenHandlerGetRolesTree_ThenReturnNull() {
        ProductionCompany company = companyWithFounderAndOwnerAndManager();

        RolesTreeDTO result = productionHandler.getRolesTree("random-user", company);

        assertNull(result);
    }

    @Test
    public void GivenCallerIsManager_WhenHandlerGetRolesTree_ThenReturnNull() {
        ProductionCompany company = companyWithFounderAndOwnerAndManager();

        RolesTreeDTO result = productionHandler.getRolesTree(MANAGER_ID, company);

        assertNull(result);
    }

    @Test
    public void GivenNullUserId_WhenHandlerGetRolesTree_ThenReturnNull() {
        ProductionCompany company = companyWithFounderAndOwnerAndManager();

        RolesTreeDTO result = productionHandler.getRolesTree(null, company);

        assertNull(result);
    }

    @Test
    public void GivenBlankUserId_WhenHandlerGetRolesTree_ThenReturnNull() {
        ProductionCompany company = companyWithFounderAndOwnerAndManager();

        RolesTreeDTO result = productionHandler.getRolesTree("   ", company);

        assertNull(result);
    }

    @Test
    public void GivenNullCompany_WhenHandlerGetRolesTree_ThenReturnNull() {
        RolesTreeDTO result = productionHandler.getRolesTree(FOUNDER_ID, null);

        assertNull(result);
    }

    // ── Concurrency tests ────────────────────────────────────────────────────

    @Test
    public void Given10ConcurrentReads_WhenGetRolesTree_ThenAllSucceed() throws InterruptedException {
        // Race condition: multiple owners reading the tree simultaneously
        // Expected: all reads succeed since this is a read-only operation
        int threads = 10;
        CountDownLatch startLatch  = new CountDownLatch(1);
        CountDownLatch doneLatch   = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);

        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenAnswer(inv ->
                Optional.of(companyWithFounderAndOwnerAndManager()));

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    RolesTreeDTO result = productionService.getRolesTree(VALID_TOKEN, COMPANY_ID);
                    if (result != null) successCount.incrementAndGet();
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

        assertEquals(threads, successCount.get());
    }
    
}