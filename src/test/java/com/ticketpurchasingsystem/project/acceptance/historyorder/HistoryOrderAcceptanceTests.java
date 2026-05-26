package com.ticketpurchasingsystem.project.acceptance.historyorder;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.HistoryOrderService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderHandler;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import com.ticketpurchasingsystem.project.infrastructure.HistoryOrderRepo;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HistoryOrderAcceptanceTests {

    private HistoryOrderService historyOrderService;
    private IHistoryOrderRepo historyOrderRepo;
    private HistoryOrderHandler historyOrderHandler;
    private AuthenticationService authenticationService;
    private ISessionRepo sessionRepo;

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";
    private static final String MEMBER_A_ID = "memberA_123";
    private Timestamp purchaseDate;
    private List<String> mockSeatIds;
    private HashMap<String, Integer> mockStandingAreaQuantities;



    @BeforeEach
    public void setup(){
        sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        authenticationService = new AuthenticationService(domainAuthService, sessionRepo);

        // Inject secret for token generation (matching the ActiveOrder tests setup)
        ReflectionTestUtils.setField(domainAuthService, "secret", TEST_SECRET);
        domainAuthService.init();

        ProductionService productionService = null; // never used in historyOrderService
        historyOrderRepo = new HistoryOrderRepo();
        historyOrderHandler = new HistoryOrderHandler();
        historyOrderService = new HistoryOrderService(historyOrderRepo, historyOrderHandler, authenticationService, productionService);

        // Initialize reusable mock data for orders
        purchaseDate = new Timestamp(System.currentTimeMillis());
        mockSeatIds = List.of("A1", "A2");
        mockStandingAreaQuantities = new HashMap<>();
    }
    @Test
    public void checkSetup(){
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
}
