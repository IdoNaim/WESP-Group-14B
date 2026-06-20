package com.ticketpurchasingsystem.project.domain.ProductionTest;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;

@ExtendWith(MockitoExtension.class)
public class DenyAppointmentTest {

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private IProdRepo prodRepo;
    @Mock
    private ProductionEventPublisher productionEventPublisher;

    private ProductionHandler productionHandler;
    private ProductionService productionService;

    private static final String VALID_TOKEN = "valid-token";
    private static final String INVALID_TOKEN = "bad-token";
    private static final String FOUNDER_ID = "eden-1";
    private static final String MANAGER_ID = "tomer-99";
    private static final Integer COMPANY_ID = 1;
    private static final Set<ManagerPermission> PERMISSIONS = EnumSet.of(
            ManagerPermission.INVENTORY_MANAGEMENT);

    @BeforeEach
    void setUp() {
        productionHandler = new ProductionHandler();
        productionService = new ProductionService(
                authenticationService, productionHandler, prodRepo, productionEventPublisher);
    }

    private ProductionCompany companyWithPendingManager() {
        ProductionCompany company = new ProductionCompany(new ProductionCompanyDTO("Test Co", "desc", "t@co.com"));
        company.setCompanyId(COMPANY_ID);
        company.initFounder(FOUNDER_ID);
        company.requestManager(FOUNDER_ID, MANAGER_ID, PERMISSIONS);
        return company;
    }

    // --- Domain-level tests ---

    @Test
    public void GivenPendingAppointment_WhenDenyAppointment_ThenRemovedAndNotAMember() {
        ProductionCompany company = companyWithPendingManager();

        boolean result = company.denyAppointment(MANAGER_ID);

        assertTrue(result);
        assertFalse(company.hasPendingAppointment(MANAGER_ID));
        assertFalse(company.isManager(MANAGER_ID));
    }

    @Test
    public void GivenNoPendingAppointment_WhenDenyAppointment_ThenReturnFalse() {
        ProductionCompany company = companyWithPendingManager();

        boolean result = company.denyAppointment("nobody");

        assertFalse(result);
    }

    // --- Handler-level tests ---

    @Test
    public void GivenPendingAppointment_WhenDenyAppointment_ThenReturnCompany() {
        ProductionCompany company = companyWithPendingManager();

        ProductionCompany result = productionHandler.denyAppointment(MANAGER_ID, company);

        assertNotNull(result);
        assertFalse(result.hasPendingAppointment(MANAGER_ID));
    }

    @Test
    public void GivenNoPendingAppointment_WhenDenyAppointment_ThenReturnNull() {
        ProductionCompany company = companyWithPendingManager();

        assertNull(productionHandler.denyAppointment("nobody", company));
    }

    @Test
    public void GivenNullArguments_WhenDenyAppointment_ThenReturnNull() {
        assertNull(productionHandler.denyAppointment(null, companyWithPendingManager()));
        assertNull(productionHandler.denyAppointment(MANAGER_ID, null));
    }

    // --- Service-level tests ---

    @Test
    public void GivenPendingAppointment_WhenDenyAppointment_ThenReturnTrueAndNoRoleEvent() {
        ProductionCompany company = companyWithPendingManager();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(MANAGER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean result = productionService.denyAppointment(VALID_TOKEN, COMPANY_ID);

        assertTrue(result);
        verify(productionEventPublisher, never()).publishAppointManagerEvent(any(), any(), any(), any());
        verify(productionEventPublisher, never()).publishAssignOwnerEvent(any(), any(), any());
    }

    @Test
    public void GivenInvalidToken_WhenDenyAppointment_ThenReturnFalse() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        boolean result = productionService.denyAppointment(INVALID_TOKEN, COMPANY_ID);

        assertFalse(result);
    }

    @Test
    public void GivenNoPendingAppointmentForUser_WhenServiceDenyAppointment_ThenReturnFalse() {
        ProductionCompany company = companyWithPendingManager();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn("stranger");
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        boolean result = productionService.denyAppointment(VALID_TOKEN, COMPANY_ID);

        assertFalse(result);
        verify(prodRepo, never()).save(any());
    }
}
