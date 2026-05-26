package com.ticketpurchasingsystem.project.acceptance.admin;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private static final String ADMIN_ID   = "sysadmin";

    private ActiveOrderMemRepo activeOrderRepo;
    private HistoryOrderRepo historyOrderRepo;
    private SystemAdminService adminService;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        activeOrderRepo = new ActiveOrderMemRepo();
        historyOrderRepo = new HistoryOrderRepo();

        InMemoryAdminRepo adminRepo = new InMemoryAdminRepo();
        AdminInfo adminInfo = new AdminInfo(ADMIN_ID, "admin@system.com");
        adminRepo.save(adminInfo);

        InMemorySessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        Field secretField = DomainAuthService.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(domainAuthService, JWT_SECRET);
        domainAuthService.init();

        AuthenticationService authService = new AuthenticationService(domainAuthService, sessionRepo);
        // token subject must match AdminInfo.getId() (auto-generated), not the username
        adminToken = authService.login(adminInfo.getId(), "admin");

        ActiveOrderListener activeOrderListener = new ActiveOrderListener(activeOrderRepo);

        AdminPublisher adminPublisher = new AdminPublisher(event -> {
            if (event instanceof GetAllActiveOrdersEvent e)
                activeOrderListener.handleGetAllActiveOrdersEvent(e);
            else if (event instanceof GetAllHistoryOrdersEvent e)
                e.setResult(historyOrderRepo.findAll());
        });

        adminService = new SystemAdminService(adminRepo, adminPublisher, authService);
    }

    // ─── getAllActiveOrders ───────────────────────────────────────────────────

    @Test
    void GivenNoActiveOrders_WhenGetAllActiveOrders_ThenReturnEmptyList() {
        List<ActiveOrderDTO> result = adminService.getAllActiveOrders(adminToken);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void GivenActiveOrdersExist_WhenGetAllActiveOrders_ThenAllOrdersAreReturned() {
        activeOrderRepo.save(new ActiveOrderItem("order-1", "user-1", "event-1"));
        activeOrderRepo.save(new ActiveOrderItem("order-2", "user-2", "event-1"));

        List<ActiveOrderDTO> result = adminService.getAllActiveOrders(adminToken);

        assertEquals(2, result.size());
    }

    @Test
    void GivenAnActiveOrder_WhenGetAllActiveOrders_ThenThatOrderIsIncluded() {
        activeOrderRepo.save(new ActiveOrderItem("order-1", "user-1", "event-1"));

        List<ActiveOrderDTO> result = adminService.getAllActiveOrders(adminToken);

        assertTrue(result.stream().anyMatch(o -> "order-1".equals(o.getOrderId())));
    }

    // ─── getAllHistoryOrders ──────────────────────────────────────────────────

    @Test
    void GivenNoHistoryOrders_WhenGetAllHistoryOrders_ThenReturnEmptyList() {
        List<HistoryOrderDTO> result = adminService.getAllHistoryOrders(adminToken);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void GivenHistoryOrdersExist_WhenGetAllHistoryOrders_ThenAllOrdersAreReturned() {
        historyOrderRepo.save(new HistoryOrderItem("h-1", "user-1", "event-1", 1, 50.0, List.of(), new HashMap<>()));
        historyOrderRepo.save(new HistoryOrderItem("h-2", "user-2", "event-1", 1, 75.0, List.of(), new HashMap<>()));

        List<HistoryOrderDTO> result = adminService.getAllHistoryOrders(adminToken);

        assertEquals(2, result.size());
    }

    @Test
    void GivenAHistoryOrder_WhenGetAllHistoryOrders_ThenThatOrderIsIncluded() {
        historyOrderRepo.save(new HistoryOrderItem("h-1", "user-1", "event-1", 1, 50.0, List.of(), new HashMap<>()));

        List<HistoryOrderDTO> result = adminService.getAllHistoryOrders(adminToken);

        assertTrue(result.stream().anyMatch(o -> "h-1".equals(o.getOrderId())));
    }
}
