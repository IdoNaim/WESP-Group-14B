package com.ticketpurchasingsystem.project.domain.ProductionTest;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Utils.ManagerDTO;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppointManagerTest {

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private IProdRepo prodRepo;
    @Mock
    private ProductionEventPublisher productionEventPublisher;
    @Captor
    private ArgumentCaptor<ProductionCompany> captor;

    private ProductionHandler productionHandler;
    private ProductionService productionService;

    private static final String VALID_TOKEN = "valid-token";
    private static final String INVALID_TOKEN = "bad-token";
    private static final String FOUNDER_ID = "eden-1";
    private static final String OWNER_ID = "itay-1";
    private static final String MANAGER_ID = "tomer-99";
    private static final Integer COMPANY_ID = 1;
    private static final Set<ManagerPermission> PERMISSIONS = EnumSet.of(
            ManagerPermission.INVENTORY_MANAGEMENT,
            ManagerPermission.PURCHASING_AND_DISCOUNT_POLICY_MANAGEMENT);

    @BeforeEach
    void setUp() {
        productionHandler = new ProductionHandler();
        productionService = new ProductionService(
                authenticationService, productionHandler, prodRepo, productionEventPublisher);
    }

    private ProductionCompany companyWithFounderAndOwner() {
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Test Co", "desc", "test@co.com");
        ProductionCompany company = new ProductionCompany(dto);
        company.setCompanyId(COMPANY_ID);
        company.initFounder(FOUNDER_ID);
        company.appointOwner(FOUNDER_ID, OWNER_ID);
        return company;
    }

    // --- Service-level tests ---

    @Test
    public void GivenValidTokenAndOwnerAsAppointerId_WhenAppointManager_ThenReturnTrue() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(productionEventPublisher.publishIsUserRegisteredEvent(MANAGER_ID)).thenReturn(true);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        boolean result = productionService.appointManager(VALID_TOKEN, COMPANY_ID, MANAGER_ID, PERMISSIONS);

        // Assert
        assertTrue(result);
    }

    @Test
    public void GivenInvalidToken_WhenAppointManager_ThenReturnFalse() {
        // Arrange
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        // Act
        boolean result = productionService.appointManager(INVALID_TOKEN, COMPANY_ID, MANAGER_ID, PERMISSIONS);

        // Assert
        assertFalse(result);
        verifyNoInteractions(prodRepo, productionEventPublisher);
    }

    @Test
    public void GivenManagerNotRegisteredUser_WhenAppointManager_ThenReturnFalse() {
        // Arrange
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(productionEventPublisher.publishIsUserRegisteredEvent(MANAGER_ID)).thenReturn(false);

        // Act
        boolean result = productionService.appointManager(VALID_TOKEN, COMPANY_ID, MANAGER_ID, PERMISSIONS);

        // Assert
        assertFalse(result);
        verifyNoInteractions(prodRepo);
    }

    @Test
    public void GivenCompanyDoesNotExist_WhenAppointManager_ThenReturnFalse() {
        // Arrange
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(productionEventPublisher.publishIsUserRegisteredEvent(MANAGER_ID)).thenReturn(true);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.empty());

        // Act
        boolean result = productionService.appointManager(VALID_TOKEN, COMPANY_ID, MANAGER_ID, PERMISSIONS);

        // Assert
        assertFalse(result);
        verify(prodRepo, never()).save(any());
    }

    @Test
    public void GivenCallerNotOwnerOrManager_WhenAppointManager_ThenReturnFalse() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn("random-user");
        when(productionEventPublisher.publishIsUserRegisteredEvent(MANAGER_ID)).thenReturn(true);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        // Act
        boolean result = productionService.appointManager(VALID_TOKEN, COMPANY_ID, MANAGER_ID, PERMISSIONS);

        // Assert
        assertFalse(result);
        verify(prodRepo, never()).save(any());
    }

    @Test
    public void GivenValidInput_WhenAppointManager_ThenRepoSaveCalledWithUpdatedCompany() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(productionEventPublisher.publishIsUserRegisteredEvent(MANAGER_ID)).thenReturn(true);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        boolean result = productionService.appointManager(VALID_TOKEN, COMPANY_ID, MANAGER_ID, PERMISSIONS);

        // Assert
        assertTrue(result);
        verify(prodRepo, times(1)).save(any());
        assertTrue(captor.getValue().isManager(MANAGER_ID),
                "Repo must receive the company with the new manager already added");
    }

    @Test
    public void GivenValidInput_WhenAppointManager_ThenAppointManagerEventIsPublished() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(productionEventPublisher.publishIsUserRegisteredEvent(MANAGER_ID)).thenReturn(true);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productionService.appointManager(VALID_TOKEN, COMPANY_ID, MANAGER_ID, PERMISSIONS);

        // Assert
        verify(productionEventPublisher).publishAppointManagerEvent(any(), eq(FOUNDER_ID), eq(MANAGER_ID),
                eq(PERMISSIONS));
    }

    // --- Handler-level tests ---

    @Test
    public void GivenOwnerAsAppointerId_WhenAppointManager_ThenReturnNonNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.appointManager(
                FOUNDER_ID, COMPANY_ID, MANAGER_ID, PERMISSIONS, company);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void GivenExistingManagerAsAppointerId_WhenAppointManager_ThenReturnNonNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        company.appointManager(FOUNDER_ID, OWNER_ID + "-mgr", EnumSet.of(ManagerPermission.INVENTORY_MANAGEMENT));
        String existingManagerId = OWNER_ID + "-mgr";

        // Act
        ProductionCompany result = productionHandler.appointManager(
                existingManagerId, COMPANY_ID, MANAGER_ID, PERMISSIONS, company);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void GivenCallerNotOwnerOrManager_WhenAppointManager_ThenReturnNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.appointManager(
                "random-user", COMPANY_ID, MANAGER_ID, PERMISSIONS, company);

        // Assert
        assertNull(result);
    }

    @Test
    public void GivenAppointeeAlreadyManager_WhenAppointManager_ThenReturnNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        company.appointManager(FOUNDER_ID, MANAGER_ID, PERMISSIONS);

        // Act
        ProductionCompany result = productionHandler.appointManager(
                FOUNDER_ID, COMPANY_ID, MANAGER_ID, PERMISSIONS, company);

        // Assert
        assertNull(result);
    }

    @Test
    public void GivenNullManagerId_WhenAppointManager_ThenReturnNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.appointManager(
                FOUNDER_ID, COMPANY_ID, null, PERMISSIONS, company);

        // Assert
        assertNull(result);
    }

    @Test
    public void GivenNullPermissions_WhenAppointManager_ThenReturnNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.appointManager(
                FOUNDER_ID, COMPANY_ID, MANAGER_ID, null, company);

        // Assert
        assertNull(result);
    }

    @Test
    public void GivenSuccessfulAppointment_WhenAppointManager_ThenReturnedCompanyContainsNewManager() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.appointManager(
                FOUNDER_ID, COMPANY_ID, MANAGER_ID, PERMISSIONS, company);

        // Assert
        assertNotNull(result);
        assertTrue(result.isManager(MANAGER_ID),
                "Returned company must contain the newly appointed manager");
    }

    @Test
    public void GivenFounderAppoints_WhenAppointManager_ThenManagerHasFounderAsAppointer() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        productionHandler.appointManager(FOUNDER_ID, COMPANY_ID, MANAGER_ID, PERMISSIONS, company);

        // Assert
        ManagerDTO node = company.getManagerDTO(MANAGER_ID).orElseThrow();
        assertEquals(FOUNDER_ID, node.getAppointerId());
    }

    @Test
    public void GivenOwnerAppoints_WhenAppointManager_ThenManagerHasOwnerAsAppointer() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        productionHandler.appointManager(OWNER_ID, COMPANY_ID, MANAGER_ID, PERMISSIONS, company);

        // Assert
        ManagerDTO node = company.getManagerDTO(MANAGER_ID).orElseThrow();
        assertEquals(OWNER_ID, node.getAppointerId());
    }

    @Test
    public void GivenPermissionsSelected_WhenAppointManager_ThenManagerHasExactPermissions() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        Set<ManagerPermission> expected = EnumSet.of(
                ManagerPermission.COMPANY_POLICY_MANAGEMENT,
                ManagerPermission.PURCHASING_AND_DISCOUNT_POLICY_MANAGEMENT);

        // Act
        productionHandler.appointManager(FOUNDER_ID, COMPANY_ID, MANAGER_ID, expected, company);

        // Assert
        ManagerDTO node = company.getManagerDTO(MANAGER_ID).orElseThrow();
        assertEquals(expected, node.getPermissions());
    }

    @Test
    public void GivenEmptyPermissions_WhenAppointManager_ThenManagerHasNoPermissions() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        Set<ManagerPermission> empty = EnumSet.noneOf(ManagerPermission.class);

        // Act
        productionHandler.appointManager(FOUNDER_ID, COMPANY_ID, MANAGER_ID, empty, company);

        // Assert
        ManagerDTO node = company.getManagerDTO(MANAGER_ID).orElseThrow();
        assertTrue(node.getPermissions().isEmpty());
    }
}
