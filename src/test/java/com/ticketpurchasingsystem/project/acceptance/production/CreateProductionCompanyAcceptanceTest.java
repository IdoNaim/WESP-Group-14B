package com.ticketpurchasingsystem.project.acceptance.production;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.application.SystemAdminService;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import com.ticketpurchasingsystem.project.infrastructure.ProdRepo;

class CreateProductionCompanyAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";

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
        ProductionEventPublisher publisher = new ProductionEventPublisher(event -> {});
        productionService = new ProductionService(authService, new ProductionHandler(), prodRepo, publisher);
    }

    @Test
    void GivenLoggedInUser_WhenCreateCompany_ThenReturnTrue() {
        // Arrange
        String token = authService.login("eden");
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Eden Events", "Great events", "eden@events.com");

        // Act
        Integer result = productionService.createProductionCompany(token, dto);

        // Assert
        assertNotNull(result);
    }

    @Test
    void GivenLoggedInUser_WhenCreateCompany_ThenCompanyIsPersistedWithCorrectFounder() {
        // Arrange
        String token = authService.login("eden");
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Eden Events", "desc", "eden@events.com");

        // Act
        productionService.createProductionCompany(token, dto);

        // Assert
        Optional<ProductionCompany> saved = prodRepo.findByName("Eden Events");
        assertTrue(saved.isPresent());
        assertEquals("eden", saved.get().getFounderId());
    }

    // Fail

    @Test
    void GivenNotLoggedIn_WhenCreateCompany_ThenReturnFalse() {
        // Arrange
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Eden Events", "desc", "eden@events.com");

        // Act
        Integer result = productionService.createProductionCompany("invalid-token", dto);

        // Assert
        assertNull(result);
    }

    @Test
    void GivenDuplicateCompanyName_WhenCreateCompany_ThenReturnFalse() {
        // Arrange
        String token = authService.login("eden");
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Duplicate Corp", "desc", "dup@corp.com");
        productionService.createProductionCompany(token, dto);

        // Act
        Integer secondResult = productionService.createProductionCompany(token, dto);

        // Assert
        assertNull(secondResult);
    }

    @Test
    void GivenBlankCompanyName_WhenCreateCompany_ThenReturnFalse() {
        // Arrange
        String token = authService.login("eden");
        ProductionCompanyDTO dto = new ProductionCompanyDTO("   ", "desc", "eden@events.com");

        // Act
        Integer result = productionService.createProductionCompany(token, dto);

        // Assert
        assertNull(result);
    }
}
