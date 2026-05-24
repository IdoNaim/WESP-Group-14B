package com.ticketpurchasingsystem.project.acceptance.production;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.application.SystemAdminService;
import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AppointManagerAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";
    private static final String FOUNDER = "founder-eden";
    private static final String MANAGER_ID = "manager-itay";
    private static final Set<ManagerPermission> PERMISSIONS = EnumSet.of(
            ManagerPermission.INVENTORY_MANAGEMENT,
            ManagerPermission.PURCHASE_AND_ORDER_HISTORY_ACCESS);

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
        authService = new AuthenticationService(domainAuthService, mock(SystemAdminService.class), sessionRepo);
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
    void GivenOwnerAppoints_WhenAppointManager_ThenReturnTrue() {
        // Arrange
        registeredUsers.add(MANAGER_ID);
        String founderToken = authService.login(FOUNDER);

        // Act
        boolean result = productionService.appointManager(founderToken, companyId, MANAGER_ID, PERMISSIONS);

        // Assert
        assertTrue(result);
    }

    @Test
    void GivenOwnerAppoints_WhenAppointManager_ThenManagerAppearsInCompany() {
        // Arrange
        registeredUsers.add(MANAGER_ID);
        String founderToken = authService.login(FOUNDER);

        // Act
        productionService.appointManager(founderToken, companyId, MANAGER_ID, PERMISSIONS);

        // Assert
        Optional<ProductionCompany> company = prodRepo.findByName("Events Co");
        assertTrue(company.isPresent());
        assertTrue(company.get().isManager(MANAGER_ID));
    }

    // Fail

    @Test
    void GivenInvalidToken_WhenAppointManager_ThenReturnFalse() {
        // Arrange
        registeredUsers.add(MANAGER_ID);

        // Act
        boolean result = productionService.appointManager("bad-token", companyId, MANAGER_ID, PERMISSIONS);

        // Assert
        assertFalse(result);
    }

    @Test
    void GivenNonOwnerAppoints_WhenAppointManager_ThenReturnFalse() {
        // Arrange
        registeredUsers.add(MANAGER_ID);
        String nonOwnerToken = authService.login("random-user");

        // Act
        boolean result = productionService.appointManager(nonOwnerToken, companyId, MANAGER_ID, PERMISSIONS);

        // Assert
        assertFalse(result);
    }

    @Test
    void GivenManagerAlreadyAppointed_WhenAppointManager_ThenReturnFalse() {
        // Arrange
        registeredUsers.add(MANAGER_ID);
        String founderToken = authService.login(FOUNDER);
        productionService.appointManager(founderToken, companyId, MANAGER_ID, PERMISSIONS);

        // Act
        String newToken = authService.login(FOUNDER);
        boolean secondResult = productionService.appointManager(newToken, companyId, MANAGER_ID, PERMISSIONS);

        // Assert
        assertFalse(secondResult);
    }

    @Test
    void GivenUnregisteredManager_WhenAppointManager_ThenReturnFalse() {
        // Arrange
        String founderToken = authService.login(FOUNDER);
        // "unregistered" is intentionally NOT added to registeredUsers

        // Act
        boolean result = productionService.appointManager(founderToken, companyId, "unregistered", PERMISSIONS);

        // Assert
        assertFalse(result);
    }
}
