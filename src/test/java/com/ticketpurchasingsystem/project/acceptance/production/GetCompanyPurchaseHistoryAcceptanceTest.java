package com.ticketpurchasingsystem.project.acceptance.production;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.GetCompanyHistoryEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import com.ticketpurchasingsystem.project.infrastructure.ProdRepo;

class GetCompanyPurchaseHistoryAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";
    private static final String FOUNDER = "founder-eden";

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
        ProductionEventPublisher publisher = new ProductionEventPublisher(event -> {
            if (event instanceof GetCompanyHistoryEvent e) {
                e.setResult(Collections.emptyList());
            }
        });
        productionService = new ProductionService(authService, new ProductionHandler(), prodRepo, publisher);

        String founderToken = authService.login(FOUNDER);
        productionService.createProductionCompany(founderToken,
                new ProductionCompanyDTO("Events Co", "desc", "events@co.com"));
        companyId = prodRepo.findByName("Events Co").get().getCompanyId();
    }

    @Test
    void GivenFounderRequestsHistory_WhenNoHistoryExists_ThenEmptyListIsReturned() {
        // Arrange
        String founderToken = authService.login(FOUNDER);

        // Act
        List<HistoryOrderItem> result = productionService.getCompanyPurchaseHistory(founderToken, companyId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // Fail

    @Test
    void GivenInvalidToken_WhenGetPurchaseHistory_ThenReturnNull() {
        // Arrange
        // (no additional setup required)

        // Act
        List<HistoryOrderItem> result = productionService.getCompanyPurchaseHistory("invalid-token", companyId);

        // Assert
        assertNull(result);
    }

    @Test
    void GivenNonOwner_WhenGetPurchaseHistory_ThenReturnNull() {
        // Arrange
        String nonOwnerToken = authService.login("random-user");

        // Act
        List<HistoryOrderItem> result = productionService.getCompanyPurchaseHistory(nonOwnerToken, companyId);

        // Assert
        assertNull(result);
    }

    @Test
    void GivenNonExistentCompany_WhenGetPurchaseHistory_ThenReturnNull() {
        // Arrange
        String founderToken = authService.login(FOUNDER);

        // Act
        List<HistoryOrderItem> result = productionService.getCompanyPurchaseHistory(founderToken, 9999);

        // Assert
        assertNull(result);
    }
}
