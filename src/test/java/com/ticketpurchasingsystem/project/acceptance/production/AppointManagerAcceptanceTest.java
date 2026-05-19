package com.ticketpurchasingsystem.project.acceptance.production;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
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

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointManagerAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";
    private static final String FOUNDER = "founder-alice";
    private static final String MANAGER_ID = "manager-charlie";
    private static final Set<ManagerPermission> PERMISSIONS = EnumSet.of(
            ManagerPermission.INVENTORY_MANAGEMENT,
            ManagerPermission.PURCHASE_AND_ORDER_HISTORY_ACCESS);

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
    void GivenOwnerAppoints_WhenAppointManager_ThenReturnTrue() {
        when(productionEventPublisher.publishIsUserRegisteredEvent(MANAGER_ID)).thenReturn(true);
        String founderToken = authService.login(FOUNDER);

        boolean result = productionService.appointManager(founderToken, companyId, MANAGER_ID, PERMISSIONS);

        assertTrue(result);
    }

    @Test
    void GivenOwnerAppoints_WhenAppointManager_ThenManagerAppearsInCompany() {
        when(productionEventPublisher.publishIsUserRegisteredEvent(MANAGER_ID)).thenReturn(true);
        String founderToken = authService.login(FOUNDER);

        productionService.appointManager(founderToken, companyId, MANAGER_ID, PERMISSIONS);

        Optional<ProductionCompany> company = prodRepo.findByName("Events Co");
        assertTrue(company.isPresent());
        assertTrue(company.get().isManager(MANAGER_ID));
    }

    @Test
    void GivenOwnerAppoints_WhenAppointManager_ThenAppointManagerEventIsPublished() {
        when(productionEventPublisher.publishIsUserRegisteredEvent(MANAGER_ID)).thenReturn(true);
        String founderToken = authService.login(FOUNDER);

        productionService.appointManager(founderToken, companyId, MANAGER_ID, PERMISSIONS);

        verify(productionEventPublisher).publishAppointManagerEvent(any(), eq(FOUNDER), eq(MANAGER_ID),
                eq(PERMISSIONS));
    }

    // Fail

    @Test
    void GivenInvalidToken_WhenAppointManager_ThenReturnFalse() {
        boolean result = productionService.appointManager("bad-token", companyId, MANAGER_ID, PERMISSIONS);

        assertFalse(result);
        verify(productionEventPublisher, never()).publishIsUserRegisteredEvent(any());
    }

    @Test
    void GivenNonOwnerAppoints_WhenAppointManager_ThenReturnFalse() {
        when(productionEventPublisher.publishIsUserRegisteredEvent(MANAGER_ID)).thenReturn(true);
        String nonOwnerToken = authService.login("random-user");

        boolean result = productionService.appointManager(nonOwnerToken, companyId, MANAGER_ID, PERMISSIONS);

        assertFalse(result);
        verify(productionEventPublisher, never()).publishAppointManagerEvent(any(), any(), any(), any());
    }

    @Test
    void GivenManagerAlreadyAppointed_WhenAppointManager_ThenReturnFalse() {
        when(productionEventPublisher.publishIsUserRegisteredEvent(MANAGER_ID)).thenReturn(true);
        String founderToken = authService.login(FOUNDER);
        productionService.appointManager(founderToken, companyId, MANAGER_ID, PERMISSIONS);

        String newToken = authService.login(FOUNDER);
        boolean secondResult = productionService.appointManager(newToken, companyId, MANAGER_ID, PERMISSIONS);

        assertFalse(secondResult);
        verify(productionEventPublisher, times(1)).publishAppointManagerEvent(any(), any(), any(), any());
    }

    @Test
    void GivenUnregisteredManager_WhenAppointManager_ThenReturnFalse() {
        when(productionEventPublisher.publishIsUserRegisteredEvent("unregistered")).thenReturn(false);
        String founderToken = authService.login(FOUNDER);

        boolean result = productionService.appointManager(founderToken, companyId, "unregistered", PERMISSIONS);

        assertFalse(result);
        verify(productionEventPublisher, never()).publishAppointManagerEvent(any(), any(), any(), any());
    }
}
