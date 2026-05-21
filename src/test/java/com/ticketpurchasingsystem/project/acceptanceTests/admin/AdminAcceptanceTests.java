package com.ticketpurchasingsystem.project.acceptanceTests.admin;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.SystemAdminService;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderListener;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.systemAdmin.AdminInfo;
import com.ticketpurchasingsystem.project.domain.systemAdmin.AdminPublisher;
import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllActiveOrdersEvent;
import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllHistoryOrdersEvent;
import com.ticketpurchasingsystem.project.infrastructure.ActiveOrderMemRepo;
import com.ticketpurchasingsystem.project.infrastructure.HistoryOrderRepo;
import com.ticketpurchasingsystem.project.infrastructure.InMemoryAdminRepo;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;

class AdminAcceptanceTests {

    private static final String JWT_SECRET = "myUltraSecretKeyForJWTSigningThatIsAtLeast32CharactersLong";

    private ActiveOrderMemRepo activeOrderRepo;
    private HistoryOrderRepo historyOrderRepo;
    private SystemAdminService systemAdminService;
    private AuthenticationService authService;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        InMemorySessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        Field secretField = DomainAuthService.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(domainAuthService, JWT_SECRET);
        domainAuthService.init();
        authService = new AuthenticationService(domainAuthService, sessionRepo);

        activeOrderRepo = new ActiveOrderMemRepo();
        historyOrderRepo = new HistoryOrderRepo();
        InMemoryAdminRepo adminRepo = new InMemoryAdminRepo();

        AdminInfo adminInfo = new AdminInfo("testAdmin", "admin@test.com");
        adminRepo.save(adminInfo);
        adminToken = authService.login(adminInfo.getId());

        ActiveOrderListener activeOrderListener = new ActiveOrderListener(activeOrderRepo);
        AdminPublisher adminPublisher = new AdminPublisher(event -> {
            if (event instanceof GetAllActiveOrdersEvent e)
                activeOrderListener.handleGetAllActiveOrdersEvent(e);
            else if (event instanceof GetAllHistoryOrdersEvent e)
                e.setResult(historyOrderRepo.findAll());
        });

        systemAdminService = new SystemAdminService(adminRepo, adminPublisher, authService);
    }

    // ─── getAllActiveOrders ───────────────────────────────────────────────────

    @Test
    void GivenNoActiveOrders_WhenGetAllActiveOrders_ThenReturnEmptyList() {
        List<ActiveOrderDTO> result = systemAdminService.getAllActiveOrders(adminToken);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void GivenActiveOrdersExist_WhenGetAllActiveOrders_ThenAllOrdersAreReturned() {
        activeOrderRepo.save(new ActiveOrderItem("order-1", "user-1", "event-1"));
        activeOrderRepo.save(new ActiveOrderItem("order-2", "user-2", "event-1"));

        List<ActiveOrderDTO> result = systemAdminService.getAllActiveOrders(adminToken);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(o -> "order-1".equals(o.getOrderId())));
        assertTrue(result.stream().anyMatch(o -> "order-2".equals(o.getOrderId())));
    }

    @Test
    void GivenAnActiveOrder_WhenGetAllActiveOrders_ThenThatOrderIsIncluded() {
        activeOrderRepo.save(new ActiveOrderItem("order-1", "user-1", "event-1"));

        List<ActiveOrderDTO> result = systemAdminService.getAllActiveOrders(adminToken);

        assertTrue(result.stream().anyMatch(o -> "order-1".equals(o.getOrderId())));
    }

    @Test
    void GivenInvalidToken_WhenGetAllActiveOrders_ThenThrowsException() {
        assertThrows(RuntimeException.class, () ->
                systemAdminService.getAllActiveOrders("bad-token"));
    }

    @Test
    void GivenNonAdminToken_WhenGetAllActiveOrders_ThenThrowsException() {
        String nonAdminToken = authService.login("regular-user");

        assertThrows(RuntimeException.class, () ->
                systemAdminService.getAllActiveOrders(nonAdminToken));
    }

    // ─── getAllHistoryOrders ──────────────────────────────────────────────────

    @Test
    void GivenNoHistoryOrders_WhenGetAllHistoryOrders_ThenReturnEmptyList() {
        List<HistoryOrderDTO> result = systemAdminService.getAllHistoryOrders(adminToken);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void GivenHistoryOrdersExist_WhenGetAllHistoryOrders_ThenAllOrdersAreReturned() {
        historyOrderRepo.save(new HistoryOrderItem("h-1", "user-1", "event-1", 1, 50.0, List.of(), new HashMap<>()));
        historyOrderRepo.save(new HistoryOrderItem("h-2", "user-2", "event-1", 1, 75.0, List.of(), new HashMap<>()));

        List<HistoryOrderDTO> result = systemAdminService.getAllHistoryOrders(adminToken);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(o -> "h-1".equals(o.getOrderId())));
        assertTrue(result.stream().anyMatch(o -> "h-2".equals(o.getOrderId())));
    }

    @Test
    void GivenAHistoryOrder_WhenGetAllHistoryOrders_ThenThatOrderIsIncluded() {
        historyOrderRepo.save(new HistoryOrderItem("h-1", "user-1", "event-1", 1, 50.0, List.of(), new HashMap<>()));

        List<HistoryOrderDTO> result = systemAdminService.getAllHistoryOrders(adminToken);

        assertTrue(result.stream().anyMatch(o -> "h-1".equals(o.getOrderId())));
    }

    @Test
    void GivenInvalidToken_WhenGetAllHistoryOrders_ThenThrowsException() {
        assertThrows(RuntimeException.class, () ->
                systemAdminService.getAllHistoryOrders("bad-token"));
    }

    @Test
    void GivenNonAdminToken_WhenGetAllHistoryOrders_ThenThrowsException() {
        String nonAdminToken = authService.login("regular-user");

        assertThrows(RuntimeException.class, () ->
                systemAdminService.getAllHistoryOrders(nonAdminToken));
    }
}
