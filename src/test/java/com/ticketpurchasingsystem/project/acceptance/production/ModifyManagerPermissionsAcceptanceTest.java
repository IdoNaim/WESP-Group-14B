package com.ticketpurchasingsystem.project.acceptance.production;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.IsUserRegisteredEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ModifyManagerPermissionsAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";
    private static final String FOUNDER = "founder-eden";
    private static final String CO_OWNER = "co-owner-tomer";
    private static final Set<ManagerPermission> PERMISSIONS = EnumSet.of(
            ManagerPermission.INVENTORY_MANAGEMENT,
            ManagerPermission.PURCHASE_AND_ORDER_HISTORY_ACCESS);

    private final Set<String> registeredUsers = new HashSet<>();

    @Autowired
    private IProdRepo prodRepo;

    @Autowired
    private ISessionRepo sessionRepo;

    private AuthenticationService authService;
    private ProductionService productionService;
    private int companyId;

    @BeforeEach
    void setUp() {
        registeredUsers.clear();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        ReflectionTestUtils.setField(domainAuthService, "secret", TEST_SECRET);
        domainAuthService.init();
        authService = new AuthenticationService(domainAuthService, sessionRepo);
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

        registeredUsers.add(CO_OWNER);
        productionService.assignOwner(founderToken, companyId, CO_OWNER);
        // Appointment now requires consent: the co-owner accepts so they are an active owner.
        productionService.acceptAppointment(authService.login(CO_OWNER), companyId);
    }

    @Test
    void GivenFounderModifiesCoOwnerPermissions_WhenModifyPermissions_ThenReturnTrue() {
        // Arrange
        String founderToken = authService.login(FOUNDER);

        // Act
        boolean result = productionService.modifyManagerPermissions(founderToken, companyId, CO_OWNER, PERMISSIONS);

        // Assert
        assertTrue(result);
    }

    @Test
    void GivenFounderModifiesCoOwnerPermissions_WhenModifyPermissions_ThenPermissionsStoredInCompany() {
        // Arrange
        String founderToken = authService.login(FOUNDER);

        // Act
        productionService.modifyManagerPermissions(founderToken, companyId, CO_OWNER, PERMISSIONS);

        // Assert
        Optional<ProductionCompany> company = prodRepo.findByName("Events Co");
        assertTrue(company.isPresent());
        assertEquals(PERMISSIONS, company.get().getManagerPermissions(CO_OWNER));
    }

    // Fail

    @Test
    void GivenInvalidToken_WhenModifyPermissions_ThenReturnFalse() {
        // Act
        boolean result = productionService.modifyManagerPermissions("bad-token", companyId, CO_OWNER, PERMISSIONS);

        // Assert
        assertFalse(result);
    }

    @Test
    void GivenNonOwnerModifies_WhenModifyPermissions_ThenReturnFalse() {
        // Arrange
        String nonOwnerToken = authService.login("random-person");

        // Act
        boolean result = productionService.modifyManagerPermissions(nonOwnerToken, companyId, CO_OWNER, PERMISSIONS);

        // Assert
        assertFalse(result);
    }

    @Test
    void GivenFounderModifiesOwnPermissions_WhenModifyPermissions_ThenReturnFalse() {
        // Arrange
        // Founder was not appointed by anyone — modifying their own permissions is not
        // allowed
        String founderToken = authService.login(FOUNDER);

        // Act
        boolean result = productionService.modifyManagerPermissions(founderToken, companyId, FOUNDER, PERMISSIONS);

        // Assert
        assertFalse(result);
    }
}
