package com.ticketpurchasingsystem.project.acceptance.historyorder;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.HistoryOrderService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderHandler;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.GetCompanyHistoryEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import com.ticketpurchasingsystem.project.infrastructure.ProdRepo;
import com.ticketpurchasingsystem.project.infrastructure.persistence.DBHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.User.IUserRepo;
import org.mockito.Mockito;

@DataJpaTest
@ActiveProfiles("test")
public class DBHistoryOrderAcceptanceTests {

    @Autowired
    private DBHistoryOrderRepo historyOrderRepo;

    private HistoryOrderService historyOrderService;
    private HistoryOrderHandler historyOrderHandler;
    private AuthenticationService authenticationService;
    private ISessionRepo sessionRepo;
    private ProductionService productionService;
    private IProdRepo prodRepo;

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";
    private static final String MEMBER_A_ID = "memberA_123";
    private Timestamp purchaseDate;
    private List<String> mockSeatIds;
    private HashMap<String, Integer> mockStandingAreaQuantities;



    @BeforeEach
    public void setup(){
        historyOrderRepo.deleteAll();
        sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        authenticationService = new AuthenticationService(domainAuthService, sessionRepo);

        // Inject secret for token generation (matching the ActiveOrder tests setup)
        ReflectionTestUtils.setField(domainAuthService, "secret", TEST_SECRET);
        domainAuthService.init();

        historyOrderHandler = new HistoryOrderHandler();

        // Set up production infrastructure for UC II.4.4 authorization tests
        prodRepo = new ProdRepo();
        ProductionHandler productionHandler = new ProductionHandler();
        // Wire the event publisher to serve history directly from historyOrderRepo
        ProductionEventPublisher productionEventPublisher = new ProductionEventPublisher(event -> {
            if (event instanceof GetCompanyHistoryEvent e) {
                e.setResult(historyOrderRepo.findAllByCompanyId(e.getCompanyId()));
            }
        });
        productionService = new ProductionService(authenticationService, productionHandler, prodRepo, productionEventPublisher);

        // Register company 10 with user "123" as founder so ownership checks work
        ProductionCompanyDTO companyDTO = new ProductionCompanyDTO("TestCompany10", "Test Company", "test@company.com");
        ProductionCompany company10 = new ProductionCompany(companyDTO);
        company10.setCompanyId(10);
        company10.initFounder("123");
        ConcurrentHashMap<Integer, ProductionCompany> storage =
                (ConcurrentHashMap<Integer, ProductionCompany>) ReflectionTestUtils.getField(prodRepo, "storage");
        storage.put(10, company10);

        IUserRepo userRepo = Mockito.mock(IUserRepo.class);
        Mockito.when(userRepo.isAdmin("admin")).thenReturn(true);
        historyOrderService = new HistoryOrderService(historyOrderRepo, historyOrderHandler, authenticationService, productionService, userRepo);

        // Initialize reusable mock data for orders
        purchaseDate = new Timestamp(System.currentTimeMillis());
        mockSeatIds = List.of("A1", "A2");
        mockStandingAreaQuantities = new HashMap<>();
    }
    @Test
    public void givenTestEnvironmentSetup_whenCheckingSetup_thenAssertionSucceeds(){
        assertTrue(true);
    }

    //UC II.3.8
    @Test
    public void GivenValidAuthenticatedUserWithOrders_WhenGetAllHistoryOrdersByUser_ThenReturnUserOrders() {
        // 1. Setup: MEMBER A is authenticated and has a purchase history
        String tokenStr = authenticationService.login(MEMBER_A_ID);
        SessionToken sessionToken = new SessionToken(tokenStr, 9999999999L);

        boolean isCreated = historyOrderService.createHistoryOrder(
                "order-001", MEMBER_A_ID, "event-10", 1, purchaseDate, 200.0, mockSeatIds, mockStandingAreaQuantities);
        assertTrue(isCreated, "Setup failed: Could not create history order");

        // 2. Action: MEMBER A requests purchase history
        List<HistoryOrderDTO> history = historyOrderService.getAllHistoryOrdersByUser(sessionToken, MEMBER_A_ID);

        // 3. Assertion: System retrieves history and returns it
        assertNotNull(history);
        assertFalse(history.isEmpty(), "History should not be empty");
        assertEquals(1, history.size());
        assertEquals("order-001", history.get(0).getOrderId());
        assertEquals(MEMBER_A_ID, history.get(0).getUserId());
    }
    @Test
    public void GivenUnauthenticatedUser_WhenGetAllHistoryOrdersByUser_ThenReturnEmptyList() {
        // 1. Setup: MEMBER A is NOT authenticated (using invalid token), but has history in DB
        historyOrderService.createHistoryOrder(
                "order-002", MEMBER_A_ID, "event-10", 1, purchaseDate, 200.0, mockSeatIds, mockStandingAreaQuantities);

        SessionToken invalidSessionToken = new SessionToken("invalid.jwt.token", 9999999999L);

        // 2. Action: Request sent to view history
        List<HistoryOrderDTO> history = historyOrderService.getAllHistoryOrdersByUser(invalidSessionToken, MEMBER_A_ID);

        // 3. Assertion: System rejects request (returns empty list based on service logic)
        assertNotNull(history);
        assertTrue(history.isEmpty(), "History should be empty when authentication fails");
    }

    @Test
    public void GivenValidAuthenticatedUserWithNoOrders_WhenGetAllHistoryOrdersByUser_ThenReturnEmptyList() {
        // 1. Setup: MEMBER A is authenticated, but has NO purchase history
        String tokenStr = authenticationService.login(MEMBER_A_ID);
        SessionToken sessionToken = new SessionToken(tokenStr, 9999999999L);

        // 2. Action: MEMBER A requests purchase history
        List<HistoryOrderDTO> history = historyOrderService.getAllHistoryOrdersByUser(sessionToken, MEMBER_A_ID);

        // 3. Assertion: System returns empty history
        assertNotNull(history);
        assertTrue(history.isEmpty(), "History should be empty for a new user with no purchases");
    }

    @Test
    public void GivenValidAuthenticatedUserWithOrdersForDeletedEvents_WhenGetAllHistoryOrdersByUser_ThenReturnUserOrdersSuccessfully() {
        // 1. Setup: MEMBER A is authenticated.
        String tokenStr = authenticationService.login(MEMBER_A_ID);
        SessionToken sessionToken = new SessionToken(tokenStr, 9999999999L);

        // 2. Setup: Some related data (e.g., the Event itself) no longer exists in the system.
        // We simulate this by assigning a hypothetical deleted event ID ("deleted_event_99").
        // Because HistoryOrderService stores snapshots via DTOs, it shouldn't crash trying to fetch missing event data.
        String deletedEventId = "deleted_event_99";
        historyOrderService.createHistoryOrder(
                "order-004", MEMBER_A_ID, deletedEventId, 1, purchaseDate, 200.0, mockSeatIds, mockStandingAreaQuantities);

        // 3. Action: MEMBER A requests purchase history
        List<HistoryOrderDTO> history = historyOrderService.getAllHistoryOrdersByUser(sessionToken, MEMBER_A_ID);

        // 4. Assertion: System successfully returns history records despite missing relational data
        assertNotNull(history);
        assertFalse(history.isEmpty(), "History should be retrieved even if event data is missing");
        assertEquals(1, history.size());
        assertEquals(deletedEventId, history.get(0).getEventId(), "The history should still retain the original event ID snapshot");
    }

    //UC II.4.4
    @Test
    public void GivenCompanyOwnerWithPurchaseHistory_WhenGetCompanyPurchaseHistory_ThenReturnCompanyOrders() {
        // 1. Setup: User 123 is authenticated and is the owner of company 10
        String token123 = authenticationService.login("123");

        // 2. Setup: Purchase history records exist for company 10 (from multiple buyers)
        int companyId = 10;
        historyOrderService.createHistoryOrder(
                "order-c10-001", "123", "event-A", companyId, purchaseDate, 150.0, mockSeatIds, mockStandingAreaQuantities);
        historyOrderService.createHistoryOrder(
                "order-c10-002", "456", "event-A", companyId, purchaseDate, 200.0, mockSeatIds, mockStandingAreaQuantities);

        // 3. Action: Company owner (user 123) requests company 10's full purchase history
        List<HistoryOrderItem> history = productionService.getCompanyPurchaseHistory(token123, companyId);

        // 4. Assertion: System retrieves and returns the full company purchase history
        assertNotNull(history);
        assertFalse(history.isEmpty(), "Company purchase history should not be empty");
        assertEquals(2, history.size());
        assertTrue(history.stream().allMatch(h -> h.getCompanyId() == companyId),
                "All returned orders should belong to company 10");
    }

    @Test
    public void GivenNonOwnerAuthenticatedUser_WhenGetCompanyPurchaseHistory_ThenOperationIsBlocked() {
        // 1. Setup: User 123 (owner of company 10) creates purchase history
        historyOrderService.createHistoryOrder(
                "order-c10-003", "123", "event-A", 10, purchaseDate, 150.0, mockSeatIds, mockStandingAreaQuantities);

        // 2. Setup: User 456 is authenticated but is NOT an owner of company 10
        String token456 = authenticationService.login("456");

        // 3. Action: Non-owner user 456 attempts to view company 10's purchase history
        List<HistoryOrderItem> history = productionService.getCompanyPurchaseHistory(token456, 10);

        // 4. Assertion: System blocks the operation — null returned for non-owners
        assertNull(history, "Non-owner should not be able to view company purchase history");
    }

    @Test
    public void GivenCompanyOwnerWithNoCompanyPurchaseHistory_WhenGetCompanyPurchaseHistory_ThenReturnEmptyList() {
        // 1. Setup: User 123 is authenticated and is the owner of company 10
        String token123 = authenticationService.login("123");

        // 2. No purchase history exists for company 10

        // 3. Action: Company owner requests company 10's purchase history
        List<HistoryOrderItem> history = productionService.getCompanyPurchaseHistory(token123, 10);

        // 4. Assertion: System returns an empty list ("no data found")
        assertNotNull(history);
        assertTrue(history.isEmpty(), "Company history should be empty when no purchases have been made for this company");
    }
    //UC II.6.3
    @Test
    public void GivenAdminFiltersByUser_WhenViewGlobalHistory_ThenReturnUserPurchaseHistory() {
        // 1. Setup: Admin is authenticated
        String adminToken = authenticationService.login("admin", "admin");
        SessionToken adminSession = new SessionToken(adminToken, 9999999999L);

        // 2. Setup: Purchase history records exist for user 456
        historyOrderService.createHistoryOrder(
                "order-g001", "456", "event-X", 10, purchaseDate, 100.0, mockSeatIds, mockStandingAreaQuantities);
        historyOrderService.createHistoryOrder(
                "order-g002", "456", "event-Y", 10, purchaseDate, 120.0, mockSeatIds, mockStandingAreaQuantities);

        // 3. Action: Admin filters global history by user 456
        List<HistoryOrderDTO> history = historyOrderService.getAllHistoryOrdersByUser(adminSession, "456");

        // 4. Assertion: System returns user 456's purchase history
        assertNotNull(history);
        assertFalse(history.isEmpty(), "Admin should be able to view user purchase history");
        assertEquals(2, history.size());
        assertTrue(history.stream().allMatch(h -> h.getUserId().equals("456")),
                "All returned orders should belong to user 456");
    }

    @Test
    public void GivenAdminFiltersByCompany_WhenViewGlobalHistory_ThenReturnCompanyPurchaseHistory() {
        // 1. Setup: Admin is authenticated
        String adminToken = authenticationService.login("admin", "admin");
        SessionToken adminSession = new SessionToken(adminToken, 9999999999L);

        // 2. Setup: Purchase history records exist for company 10
        historyOrderService.createHistoryOrder(
                "order-g003", "123", "event-X", 10, purchaseDate, 150.0, mockSeatIds, mockStandingAreaQuantities);
        historyOrderService.createHistoryOrder(
                "order-g004", "456", "event-X", 10, purchaseDate, 200.0, mockSeatIds, mockStandingAreaQuantities);

        // 3. Action: Admin filters global history by company 10
        List<HistoryOrderDTO> history = historyOrderService.getAllHistoryOrdersByCompany(adminSession, 10);

        // 4. Assertion: System returns company 10's full purchase history
        assertNotNull(history);
        assertFalse(history.isEmpty(), "Admin should be able to view company purchase history");
        assertEquals(2, history.size());
        assertTrue(history.stream().allMatch(h -> h.getCompanyId() == 10),
                "All returned orders should belong to company 10");
    }

    @Test
    public void GivenNonAdminViewsAnotherUserHistory_WhenUnauthorized_ThenAccessBlocked() {
        // 1. Setup: User 123 is authenticated (not admin)
        String token123 = authenticationService.login("123");
        SessionToken session123 = new SessionToken(token123, 9999999999L);

        // 2. Setup: Purchase history exists for user 456
        historyOrderService.createHistoryOrder(
                "order-g005", "456", "event-X", 10, purchaseDate, 100.0, mockSeatIds, mockStandingAreaQuantities);

        // 3. Action: Non-admin user 123 tries to view user 456's history
        List<HistoryOrderDTO> history = historyOrderService.getAllHistoryOrdersByUser(session123, "456");

        // 4. Assertion: System blocks access — returns empty list
        assertNotNull(history);
        assertTrue(history.isEmpty(), "Non-admin user should not be able to view another user's purchase history");
    }

    @Test
    public void GivenNonAdminNonOwnerViewsCompanyHistory_WhenUnauthorized_ThenAccessBlocked() {
        // 1. Setup: User 456 is authenticated (not admin, not owner of company 10)
        String token456 = authenticationService.login("456");

        // 2. Setup: Purchase history exists for company 10
        historyOrderService.createHistoryOrder(
                "order-g006", "123", "event-X", 10, purchaseDate, 150.0, mockSeatIds, mockStandingAreaQuantities);

        // 3. Action: Non-owner user 456 tries to view company 10's purchase history
        List<HistoryOrderItem> history = productionService.getCompanyPurchaseHistory(token456, 10);

        // 4. Assertion: System blocks access — returns null
        assertNull(history, "Non-admin non-owner should not be able to view company purchase history");
    }

    @Test
    public void GivenAdminFiltersByUserWithNoOrders_WhenViewGlobalHistory_ThenReturnEmptyList() {
        // 1. Setup: Admin is authenticated
        String adminToken = authenticationService.login("admin", "admin");
        SessionToken adminSession = new SessionToken(adminToken, 9999999999L);

        // 2. No purchase history exists for "no-orders-user"

        // 3. Action: Admin filters global history by user with no orders
        List<HistoryOrderDTO> history = historyOrderService.getAllHistoryOrdersByUser(adminSession, "no-orders-user");

        // 4. Assertion: System returns empty list
        assertNotNull(history);
        assertTrue(history.isEmpty(), "Admin should receive empty list when user has no purchase history");
    }

    @Test
    public void GivenAdminFiltersByCompanyWithNoOrders_WhenViewGlobalHistory_ThenReturnEmptyList() {
        // 1. Setup: Admin is authenticated
        String adminToken = authenticationService.login("admin", "admin");
        SessionToken adminSession = new SessionToken(adminToken, 9999999999L);

        // 2. No purchase history exists for company 10

        // 3. Action: Admin filters global history by company 10 with no orders
        List<HistoryOrderDTO> history = historyOrderService.getAllHistoryOrdersByCompany(adminSession, 10);

        // 4. Assertion: System returns empty list
        assertNotNull(history);
        assertTrue(history.isEmpty(), "Admin should receive empty list when company has no purchase history");
    }

    // ─── Concurrency Tests ───────────────────────────────────────────────────

    @Test
    public void GivenMultipleUsers_WhenConcurrentCreateHistoryOrder_ThenAllOrdersAreSaved() throws Exception {
        int threadCount = 3;
        String[] userIds = {"itay", "eden", "tomer"};
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final String userId = userIds[i];
            final String orderId = "order-" + userId;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    boolean saved = historyOrderService.createHistoryOrder(
                            orderId, userId, "event-10", 10,
                            new Timestamp(System.currentTimeMillis()),
                            100.0, List.of("A1"), new HashMap<>());
                    if (saved) successCount.incrementAndGet();
                    else failureCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Concurrent history order creation must complete without deadlock");
        executor.shutdown();

        assertEquals(3, successCount.get(), "All three orders must be saved successfully");
        assertNotNull(historyOrderRepo.findByOrderId("order-itay"), "itay's order must exist");
        assertNotNull(historyOrderRepo.findByOrderId("order-eden"), "eden's order must exist");
        assertNotNull(historyOrderRepo.findByOrderId("order-tomer"), "tomer's order must exist");
    }

    @Test
    public void GivenSameOrderId_WhenConcurrentCreate_ThenExactlyOneOrderExists() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);

        // Two threads try to save different data under the same orderId
        new Thread(() -> {
            try {
                startLatch.await();
                historyOrderService.createHistoryOrder(
                        "duplicate-order", "itay", "event-10", 10,
                        new Timestamp(System.currentTimeMillis()),
                        100.0, List.of("A1"), new HashMap<>());
                successCount.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        }).start();

        new Thread(() -> {
            try {
                startLatch.await();
                historyOrderService.createHistoryOrder(
                        "duplicate-order", "eden", "event-10", 10,
                        new Timestamp(System.currentTimeMillis()),
                        200.0, List.of("B1"), new HashMap<>());
                successCount.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        }).start();

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Concurrent create must complete without deadlock");

        HistoryOrderItem stored = historyOrderRepo.findByOrderId("duplicate-order");
        assertNotNull(stored, "The order must exist after concurrent creation");
        // DB unique constraint ensures exactly one entry is stored per orderId — no duplicates allowed
        assertEquals(1, historyOrderRepo.findAll().stream()
                .filter(o -> "duplicate-order".equals(o.getOrderId())).count(),
                "Exactly one history order with the duplicate orderId must exist — no duplicates allowed");
    }

    @Test
    public void GivenConcurrentReadAndWrite_WhenMultipleUsers_ThenReadsReturnConsistentData() throws Exception {
        // Pre-populate
        historyOrderService.createHistoryOrder(
                "pre-order-itay", "itay", "event-10", 10,
                new Timestamp(System.currentTimeMillis()), 50.0, List.of(), new HashMap<>());

        String tomerToken = authenticationService.login("tomer");
        SessionToken tomerSession = new SessionToken(tomerToken, 9999999999L);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(3);
        CopyOnWriteArrayList<Integer> readSizes = new CopyOnWriteArrayList<>();
        AtomicInteger writeErrors = new AtomicInteger(0);

        // Writer thread: eden creates a new order
        new Thread(() -> {
            try {
                startLatch.await();
                historyOrderService.createHistoryOrder(
                        "concurrent-order-eden", "eden", "event-10", 10,
                        new Timestamp(System.currentTimeMillis()), 150.0, List.of("C1"), new HashMap<>());
            } catch (Exception e) {
                writeErrors.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        }).start();

        // Reader threads: tomer reads his own orders (may be empty, but must not throw)
        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    List<HistoryOrderDTO> result =
                            historyOrderService.getAllHistoryOrdersByUser(tomerSession, "tomer");
                    readSizes.add(result != null ? result.size() : -1);
                } catch (Exception e) {
                    readSizes.add(-1);
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Concurrent read/write must complete without deadlock");

        assertEquals(0, writeErrors.get(), "Writer thread must not throw an exception");
        for (int size : readSizes) {
            assertTrue(size >= 0, "Reader must return a valid non-null list — no exception");
        }
    }
}
