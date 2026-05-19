package com.ticketpurchasingsystem.project.acceptance.production;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import com.ticketpurchasingsystem.project.infrastructure.ProdRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCompanyPurchaseHistoryAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";
    private static final String FOUNDER = "founder-alice";

    @Mock
    private ProductionEventPublisher productionEventPublisher;

    private AuthenticationService authService;
    private ProdRepo prodRepo;
    private ProductionService productionService;
    private int companyId;

    @BeforeEach
    void setUp() {
        InMemorySessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        ReflectionTestUtils.setField(domainAuthService, "secret", TEST_SECRET);
        domainAuthService.init();
        authService = new AuthenticationService(domainAuthService, sessionRepo);
        prodRepo = new ProdRepo();
        productionService = new ProductionService(authService, new ProductionHandler(), prodRepo,
                productionEventPublisher);

        String founderToken = authService.login(FOUNDER);
        productionService.createProductionCompany(founderToken,
                new ProductionCompanyDTO("Events Co", "desc", "events@co.com"));
        companyId = prodRepo.findByName("Events Co").get().getCompanyId();
    }

    @Test
    void GivenFounderRequestsHistory_WhenGetPurchaseHistory_ThenHistoryIsReturned() {
        HistoryOrderItem item = new HistoryOrderItem(
                "order-1", FOUNDER, "event-1", 100.0, new ArrayList<>(), new HashMap<>());
        when(productionEventPublisher.publishGetCompanyHistoryEvent(companyId)).thenReturn(List.of(item));
        String founderToken = authService.login(FOUNDER);

        List<HistoryOrderItem> result = productionService.getCompanyPurchaseHistory(founderToken, companyId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("order-1", result.get(0).getOrderId());
        verify(productionEventPublisher).publishGetCompanyHistoryEvent(companyId);
    }

    @Test
    void GivenFounderRequestsHistory_WhenNoHistoryExists_ThenEmptyListIsReturned() {
        when(productionEventPublisher.publishGetCompanyHistoryEvent(companyId)).thenReturn(null);
        String founderToken = authService.login(FOUNDER);

        List<HistoryOrderItem> result = productionService.getCompanyPurchaseHistory(founderToken, companyId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void GivenInvalidToken_WhenGetPurchaseHistory_ThenReturnNull() {
        List<HistoryOrderItem> result = productionService.getCompanyPurchaseHistory("invalid-token", companyId);

        assertNull(result);
        verify(productionEventPublisher, never()).publishGetCompanyHistoryEvent(anyInt());
    }

    @Test
    void GivenNonOwner_WhenGetPurchaseHistory_ThenReturnNull() {
        String nonOwnerToken = authService.login("random-user");

        List<HistoryOrderItem> result = productionService.getCompanyPurchaseHistory(nonOwnerToken, companyId);

        assertNull(result);
        verify(productionEventPublisher, never()).publishGetCompanyHistoryEvent(anyInt());
    }

    @Test
    void GivenNonExistentCompany_WhenGetPurchaseHistory_ThenReturnNull() {
        String founderToken = authService.login(FOUNDER);

        List<HistoryOrderItem> result = productionService.getCompanyPurchaseHistory(founderToken, 9999);

        assertNull(result);
        verify(productionEventPublisher, never()).publishGetCompanyHistoryEvent(anyInt());
    }
}
