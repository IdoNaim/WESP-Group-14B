package com.ticketpurchasingsystem.project.acceptance.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketpurchasingsystem.project.Controllers.HistoryOrderController;
import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.HistoryOrderService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderHandler;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.GetCompanyHistoryEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;
import com.ticketpurchasingsystem.project.infrastructure.HistoryOrderRepo;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import com.ticketpurchasingsystem.project.infrastructure.ProdRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HistoryOrderApiAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";
    private static final String USER_ID = "history-api-user-123";
    private static final int COMPANY_ID = 77;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private HistoryOrderService historyOrderService;
    private AuthenticationService authService;

    private String validAuthHeader;
    private String adminAuthHeader;

    @BeforeEach
    void setUp() {
        ISessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        authService = new AuthenticationService(domainAuthService, sessionRepo);
        ReflectionTestUtils.setField(domainAuthService, "secret", TEST_SECRET);
        domainAuthService.init();

        String userToken = authService.login(USER_ID);
        validAuthHeader = "Bearer " + userToken;
        String adminToken = authService.login("admin", "admin");
        adminAuthHeader = "Bearer " + adminToken;

        IHistoryOrderRepo historyRepo = new HistoryOrderRepo();

        IProdRepo prodRepo = new ProdRepo();
        ProductionHandler productionHandler = new ProductionHandler();
        ProductionEventPublisher productionEventPublisher = new ProductionEventPublisher(event -> {
            if (event instanceof GetCompanyHistoryEvent e) {
                e.setResult(historyRepo.findAllByCompanyId(e.getCompanyId()));
            }
        });
        ProductionService productionService = new ProductionService(
                authService, productionHandler, prodRepo, productionEventPublisher);

        // Register COMPANY_ID with USER_ID as founder so ownership checks pass
        ProductionCompanyDTO companyDTO = new ProductionCompanyDTO("TestCompany", "Test", "test@test.com");
        ProductionCompany company = new ProductionCompany(companyDTO);
        company.setCompanyId(COMPANY_ID);
        company.initFounder(USER_ID);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<Integer, ProductionCompany> storage =
                (ConcurrentHashMap<Integer, ProductionCompany>) ReflectionTestUtils.getField(prodRepo, "storage");
        storage.put(COMPANY_ID, company);

        historyOrderService = new HistoryOrderService(
                historyRepo, new HistoryOrderHandler(), authService, productionService);

        mockMvc = MockMvcBuilders.standaloneSetup(
                new HistoryOrderController(historyOrderService)).build();
        objectMapper = new ObjectMapper();
    }

    // GET /api/history/{orderId}

    @Test
    void GivenExistingOrder_WhenGetHistoryOrder_ThenReturn200WithData() throws Exception {
        historyOrderService.createHistoryOrder("order-h001", USER_ID, "event-1", COMPANY_ID,
                new Timestamp(System.currentTimeMillis()), 100.0,
                List.of("A1", "A2"), new HashMap<>());

        mockMvc.perform(get("/api/history/order-h001")
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-h001"))
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.eventId").value("event-1"));
    }

    @Test
    void GivenNonExistentOrder_WhenGetHistoryOrder_ThenReturn404() throws Exception {
        mockMvc.perform(get("/api/history/non-existent-order")
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isNotFound());
    }

    // GET /api/history?userId={userId}

    @Test
    void GivenUserWithOrders_WhenGetUserHistory_ThenReturn200WithOrders() throws Exception {
        historyOrderService.createHistoryOrder("order-h002", USER_ID, "event-2", COMPANY_ID,
                new Timestamp(System.currentTimeMillis()), 50.0, Collections.emptyList(), new HashMap<>());
        historyOrderService.createHistoryOrder("order-h003", USER_ID, "event-3", COMPANY_ID,
                new Timestamp(System.currentTimeMillis()), 75.0, Collections.emptyList(), new HashMap<>());

        mockMvc.perform(get("/api/history")
                        .param("userId", USER_ID)
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void GivenUserWithNoOrders_WhenGetUserHistory_ThenReturn200WithEmptyList() throws Exception {
        mockMvc.perform(get("/api/history")
                        .param("userId", USER_ID)
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void GivenNonOwnerRequestsOtherUserHistory_WhenGetUserHistory_ThenReturn200WithEmptyList() throws Exception {
        historyOrderService.createHistoryOrder("order-h004", "other-user", "event-4", COMPANY_ID,
                new Timestamp(System.currentTimeMillis()), 90.0, Collections.emptyList(), new HashMap<>());

        mockMvc.perform(get("/api/history")
                        .param("userId", "other-user")
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // GET /api/history?companyId={companyId}

    @Test
    void GivenCompanyOwnerWithOrders_WhenGetCompanyHistory_ThenReturn200WithOrders() throws Exception {
        historyOrderService.createHistoryOrder("order-h005", USER_ID, "event-5", COMPANY_ID,
                new Timestamp(System.currentTimeMillis()), 120.0, Collections.emptyList(), new HashMap<>());
        historyOrderService.createHistoryOrder("order-h006", "buyer-2", "event-5", COMPANY_ID,
                new Timestamp(System.currentTimeMillis()), 80.0, Collections.emptyList(), new HashMap<>());

        mockMvc.perform(get("/api/history")
                        .param("companyId", String.valueOf(COMPANY_ID))
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void GivenCompanyWithNoOrders_WhenGetCompanyHistory_ThenReturn200WithEmptyList() throws Exception {
        mockMvc.perform(get("/api/history")
                        .param("companyId", String.valueOf(COMPANY_ID))
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // GET /api/history (admin only)

    @Test
    void GivenAdmin_WhenGetAllOrders_ThenReturn200WithAllOrders() throws Exception {
        historyOrderService.createHistoryOrder("order-h007", USER_ID, "event-6", COMPANY_ID,
                new Timestamp(System.currentTimeMillis()), 60.0, Collections.emptyList(), new HashMap<>());
        historyOrderService.createHistoryOrder("order-h008", "other-user", "event-7", COMPANY_ID,
                new Timestamp(System.currentTimeMillis()), 70.0, Collections.emptyList(), new HashMap<>());

        mockMvc.perform(get("/api/history")
                        .header("Authorization", adminAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
