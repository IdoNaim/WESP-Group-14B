package com.ticketpurchasingsystem.project.acceptance.production;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.IsUserRegisteredEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import com.ticketpurchasingsystem.project.infrastructure.ProdRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AssignOwnerAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";
    private static final String FOUNDER = "founder-eden";
    private static final String NEW_OWNER = "new-owner-tomer";

    private final Set<String> registeredUsers = new HashSet<>();

    private AuthenticationService authService;
    private ProdRepo prodRepo;
    private ProductionService productionService;
    private int companyId;

    @BeforeEach
    void setUp() {
        registeredUsers.clear();
        InMemorySessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        ReflectionTestUtils.setField(domainAuthService, "secret", TEST_SECRET);
        domainAuthService.init();
        authService = new AuthenticationService(domainAuthService, sessionRepo);
        prodRepo = new ProdRepo();
        ProductionEventPublisher publisher = new ProductionEventPublisher(event -> {
            if (event instanceof IsUserRegisteredEvent e) {
                e.setRegistered(registeredUsers.contains(e.getUserId()));
            }
        });
        productionService = new ProductionService(authService, new ProductionHandler(), prodRepo, publisher);

        String founderToken = authService.login(FOUNDER);
        productionService.createProductionCompany(founderToken,
                new ProductionCompanyDTO("Events Co", "desc", "events@co.com"));
        companyId = prodRepo.findByName("Events Co").get().getCompanyId();
    }

    @Test
    void GivenFounderAssignsOwner_WhenAssignOwner_ThenReturnTrue() {
        // Arrange
        registeredUsers.add(NEW_OWNER);
        String founderToken = authService.login(FOUNDER);

        // Act
        boolean result = productionService.assignOwner(founderToken, companyId, NEW_OWNER);

        // Assert
        assertTrue(result);
    }

    @Test
    void GivenFounderAssignsOwner_WhenAssignOwner_ThenNewOwnerAppearsInCompany() {
        // Arrange
        registeredUsers.add(NEW_OWNER);
        String founderToken = authService.login(FOUNDER);

        // Act
        productionService.assignOwner(founderToken, companyId, NEW_OWNER);

        // Assert
        Optional<ProductionCompany> company = prodRepo.findByName("Events Co");
        assertTrue(company.isPresent());
        assertTrue(company.get().isOwner(NEW_OWNER));
    }

    // Fail

    @Test
    void GivenInvalidToken_WhenAssignOwner_ThenReturnFalse() {
        // Arrange
        registeredUsers.add(NEW_OWNER);

        // Act
        boolean result = productionService.assignOwner("invalid-token", companyId, NEW_OWNER);

        // Assert
        assertFalse(result);
    }

    @Test
    void GivenNonOwnerAttemptsAssignOwner_WhenAssignOwner_ThenReturnFalse() {
        // Arrange
        registeredUsers.add(NEW_OWNER);
        String nonOwnerToken = authService.login("random-user");

        // Act
        boolean result = productionService.assignOwner(nonOwnerToken, companyId, NEW_OWNER);

        // Assert
        assertFalse(result);
    }

    @Test
    void GivenAlreadyOwner_WhenAssignOwner_ThenReturnFalse() {
        // Arrange
        registeredUsers.add(FOUNDER);
        String founderToken = authService.login(FOUNDER);

        // Act
        boolean result = productionService.assignOwner(founderToken, companyId, FOUNDER);

        // Assert
        assertFalse(result);
    }

    @Test
    void GivenUnregisteredUser_WhenAssignOwner_ThenReturnFalse() {
        // Arrange
        String founderToken = authService.login(FOUNDER);

        // Act
        boolean result = productionService.assignOwner(founderToken, companyId, "unregistered-user");

        // Assert
        assertFalse(result);
    }
}
