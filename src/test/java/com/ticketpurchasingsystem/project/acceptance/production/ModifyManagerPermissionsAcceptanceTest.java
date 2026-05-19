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
class ModifyManagerPermissionsAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";
    private static final String FOUNDER = "founder-alice";
    private static final String CO_OWNER = "co-owner-bob";
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

        // Founder creates company and assigns a co-owner
        String founderToken = authService.login(FOUNDER);
        productionService.createProductionCompany(founderToken,
                new ProductionCompanyDTO("Events Co", "desc", "events@co.com"));
        companyId = prodRepo.findByName("Events Co").get().getCompanyId();

        when(productionEventPublisher.publishIsUserRegisteredEvent(CO_OWNER)).thenReturn(true);
        productionService.assignOwner(founderToken, companyId, CO_OWNER);
    }

    @Test
    void GivenFounderModifiesCoOwnerPermissions_WhenModifyPermissions_ThenReturnTrue() {
        String founderToken = authService.login(FOUNDER);

        boolean result = productionService.modifyManagerPermissions(founderToken, companyId, CO_OWNER, PERMISSIONS);

        assertTrue(result);
    }

    @Test
    void GivenFounderModifiesCoOwnerPermissions_WhenModifyPermissions_ThenPermissionsStoredInCompany() {
        String founderToken = authService.login(FOUNDER);

        productionService.modifyManagerPermissions(founderToken, companyId, CO_OWNER, PERMISSIONS);

        Optional<ProductionCompany> company = prodRepo.findByName("Events Co");
        assertTrue(company.isPresent());
        assertEquals(PERMISSIONS, company.get().getManagerPermissions(CO_OWNER));
    }

    @Test
    void GivenFounderModifiesPermissions_WhenModifyPermissions_ThenEventIsPublished() {
        String founderToken = authService.login(FOUNDER);

        productionService.modifyManagerPermissions(founderToken, companyId, CO_OWNER, PERMISSIONS);

        verify(productionEventPublisher).publishModifyManagerPermissionsEvent(
                any(), eq(FOUNDER), eq(CO_OWNER), eq(PERMISSIONS));
    }

    // fail
    @Test
    void GivenInvalidToken_WhenModifyPermissions_ThenReturnFalse() {
        boolean result = productionService.modifyManagerPermissions("bad-token", companyId, CO_OWNER, PERMISSIONS);

        assertFalse(result);
        verify(productionEventPublisher, never()).publishModifyManagerPermissionsEvent(any(), any(), any(), any());
    }

    @Test
    void GivenNonOwnerModifies_WhenModifyPermissions_ThenReturnFalse() {
        String nonOwnerToken = authService.login("random-person");

        boolean result = productionService.modifyManagerPermissions(nonOwnerToken, companyId, CO_OWNER, PERMISSIONS);

        assertFalse(result);
        verify(productionEventPublisher, never()).publishModifyManagerPermissionsEvent(any(), any(), any(), any());
    }

    @Test
    void GivenFounderModifiesOwnPermissions_WhenModifyPermissions_ThenReturnFalse() {
        // Founder was not appointed by anyone
        // returns false
        String founderToken = authService.login(FOUNDER);

        boolean result = productionService.modifyManagerPermissions(founderToken, companyId, FOUNDER, PERMISSIONS);

        assertFalse(result);
        verify(productionEventPublisher, never()).publishModifyManagerPermissionsEvent(any(), any(), any(), any());
    }
}
