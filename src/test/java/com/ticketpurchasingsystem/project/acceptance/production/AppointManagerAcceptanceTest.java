package com.ticketpurchasingsystem.project.acceptance.production;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
class AppointManagerAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";
    private static final String FOUNDER = "founder-eden";
    private static final String MANAGER_ID = "manager-itay";
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
    }

    @Test
    void GivenOwnerAppoints_WhenAppointManager_ThenReturnTrue() {
        registeredUsers.add(MANAGER_ID);
        String founderToken = authService.login(FOUNDER);

        boolean result = productionService.appointManager(founderToken, companyId, MANAGER_ID, PERMISSIONS);

        assertTrue(result);
    }

    @Test
    void GivenOwnerAppoints_WhenAppointManager_ThenManagerAppearsInCompany() {
        registeredUsers.add(MANAGER_ID);
        String founderToken = authService.login(FOUNDER);

        productionService.appointManager(founderToken, companyId, MANAGER_ID, PERMISSIONS);

        Optional<ProductionCompany> company = prodRepo.findByName("Events Co");
        assertTrue(company.isPresent());
        assertTrue(company.get().isManager(MANAGER_ID));
    }

    @Test
    void GivenInvalidToken_WhenAppointManager_ThenReturnFalse() {
        registeredUsers.add(MANAGER_ID);

        boolean result = productionService.appointManager("bad-token", companyId, MANAGER_ID, PERMISSIONS);

        assertFalse(result);
    }

    @Test
    void GivenNonOwnerAppoints_WhenAppointManager_ThenReturnFalse() {
        registeredUsers.add(MANAGER_ID);
        String nonOwnerToken = authService.login("random-user");

        boolean result = productionService.appointManager(nonOwnerToken, companyId, MANAGER_ID, PERMISSIONS);

        assertFalse(result);
    }

    @Test
    void GivenManagerAlreadyAppointed_WhenAppointManager_ThenReturnFalse() {
        registeredUsers.add(MANAGER_ID);
        String founderToken = authService.login(FOUNDER);
        productionService.appointManager(founderToken, companyId, MANAGER_ID, PERMISSIONS);

        String newToken = authService.login(FOUNDER);
        boolean secondResult = productionService.appointManager(newToken, companyId, MANAGER_ID, PERMISSIONS);

        assertFalse(secondResult);
    }

    @Test
    void GivenUnregisteredManager_WhenAppointManager_ThenReturnFalse() {
        String founderToken = authService.login(FOUNDER);

        boolean result = productionService.appointManager(founderToken, companyId, "unregistered", PERMISSIONS);

        assertFalse(result);
    }
}
