package com.ticketpurchasingsystem.project.acceptance.production;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateProductionCompanyAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";

    @Mock
    private ProductionEventPublisher productionEventPublisher;

    private AuthenticationService authService;
    private ProdRepo prodRepo;
    private ProductionService productionService;

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
    }

    @Test
    void GivenLoggedInUser_WhenCreateCompany_ThenReturnTrue() {
        String token = authService.login("alice");
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Alice Events", "Great events", "alice@events.com");

        boolean result = productionService.createProductionCompany(token, dto);

        assertTrue(result);
    }

    @Test
    void GivenLoggedInUser_WhenCreateCompany_ThenCompanyIsPersistedWithCorrectFounder() {
        String token = authService.login("alice");
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Alice Events", "desc", "alice@events.com");

        productionService.createProductionCompany(token, dto);

        Optional<ProductionCompany> saved = prodRepo.findByName("Alice Events");
        assertTrue(saved.isPresent());
        assertEquals("alice", saved.get().getFounderId());
    }

    @Test
    void GivenLoggedInUser_WhenCreateCompany_ThenNewProdEventIsPublished() {
        String token = authService.login("alice");
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Alice Events", "desc", "alice@events.com");

        productionService.createProductionCompany(token, dto);

        verify(productionEventPublisher, times(1)).publishNewProdEvent(any());
    }

    // Fail

    @Test
    void GivenNotLoggedIn_WhenCreateCompany_ThenReturnFalse() {
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Alice Events", "desc", "alice@events.com");

        boolean result = productionService.createProductionCompany("invalid-token", dto);

        assertFalse(result);
        verify(productionEventPublisher, never()).publishNewProdEvent(any());
    }

    @Test
    void GivenDuplicateCompanyName_WhenCreateCompany_ThenReturnFalse() {
        String token = authService.login("alice");
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Duplicate Corp", "desc", "dup@corp.com");
        productionService.createProductionCompany(token, dto);

        boolean secondResult = productionService.createProductionCompany(token, dto);

        assertFalse(secondResult);
        verify(productionEventPublisher, times(1)).publishNewProdEvent(any());
    }

    @Test
    void GivenBlankCompanyName_WhenCreateCompany_ThenReturnFalse() {
        String token = authService.login("alice");
        ProductionCompanyDTO dto = new ProductionCompanyDTO("   ", "desc", "alice@events.com");

        boolean result = productionService.createProductionCompany(token, dto);

        assertFalse(result);
        verify(productionEventPublisher, never()).publishNewProdEvent(any());
    }
}
