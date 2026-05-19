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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignOwnerAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";
    private static final String FOUNDER = "founder-alice";
    private static final String NEW_OWNER = "new-owner-bob";

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
    void GivenFounderAssignsOwner_WhenAssignOwner_ThenReturnTrue() {
        when(productionEventPublisher.publishIsUserRegisteredEvent(NEW_OWNER)).thenReturn(true);
        String founderToken = authService.login(FOUNDER);

        boolean result = productionService.assignOwner(founderToken, companyId, NEW_OWNER);

        assertTrue(result);
    }

    @Test
    void GivenFounderAssignsOwner_WhenAssignOwner_ThenNewOwnerAppearsInCompany() {
        when(productionEventPublisher.publishIsUserRegisteredEvent(NEW_OWNER)).thenReturn(true);
        String founderToken = authService.login(FOUNDER);

        productionService.assignOwner(founderToken, companyId, NEW_OWNER);

        Optional<ProductionCompany> company = prodRepo.findByName("Events Co");
        assertTrue(company.isPresent());
        assertTrue(company.get().isOwner(NEW_OWNER));
    }

    @Test
    void GivenFounderAssignsOwner_WhenAssignOwner_ThenAssignOwnerEventIsPublished() {
        when(productionEventPublisher.publishIsUserRegisteredEvent(NEW_OWNER)).thenReturn(true);
        String founderToken = authService.login(FOUNDER);

        productionService.assignOwner(founderToken, companyId, NEW_OWNER);

        verify(productionEventPublisher).publishAssignOwnerEvent(any(), eq(FOUNDER), eq(NEW_OWNER));
    }

    // Fail

    @Test
    void GivenInvalidToken_WhenAssignOwner_ThenReturnFalse() {
        boolean result = productionService.assignOwner("invalid-token", companyId, NEW_OWNER);

        assertFalse(result);
        verify(productionEventPublisher, never()).publishIsUserRegisteredEvent(any());
    }

    @Test
    void GivenNonOwnerAttemptsAssignOwner_WhenAssignOwner_ThenReturnFalse() {
        when(productionEventPublisher.publishIsUserRegisteredEvent(NEW_OWNER)).thenReturn(true);
        String nonOwnerToken = authService.login("random-user");

        boolean result = productionService.assignOwner(nonOwnerToken, companyId, NEW_OWNER);

        assertFalse(result);
        verify(productionEventPublisher, never()).publishAssignOwnerEvent(any(), any(), any());
    }

    @Test
    void GivenAlreadyOwner_WhenAssignOwner_ThenReturnFalse() {
        when(productionEventPublisher.publishIsUserRegisteredEvent(FOUNDER)).thenReturn(true);
        String founderToken = authService.login(FOUNDER);

        boolean result = productionService.assignOwner(founderToken, companyId, FOUNDER);

        assertFalse(result);
        verify(productionEventPublisher, never()).publishAssignOwnerEvent(any(), any(), any());
    }

    @Test
    void GivenUnregisteredUser_WhenAssignOwner_ThenReturnFalse() {
        when(productionEventPublisher.publishIsUserRegisteredEvent("unregistered-user")).thenReturn(false);
        String founderToken = authService.login(FOUNDER);

        boolean result = productionService.assignOwner(founderToken, companyId, "unregistered-user");

        assertFalse(result);
        verify(productionEventPublisher, never()).publishAssignOwnerEvent(any(), any(), any());
    }
}
