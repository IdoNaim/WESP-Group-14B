package com.ticketpurchasingsystem.project.domain.ProductionTest;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Utils.OwnerDTO;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AssignOwnerTest {

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
    private static final String APPOINTEE_ID = "tomer-99";
    private static final Integer COMPANY_ID = 1;

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

    @Test
    public void GivenValidTokenAndValidAppointee_WhenAssignOwner_ThenReturnTrue() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(productionEventPublisher.publishIsUserRegisteredEvent(APPOINTEE_ID)).thenReturn(true);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        boolean result = productionService.assignOwner(VALID_TOKEN, COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertTrue(result);
    }

    @Test
    public void GivenInvalidToken_WhenAssignOwner_ThenReturnFalse() {
        // Arrange
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        // Act
        boolean result = productionService.assignOwner(INVALID_TOKEN, COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertFalse(result);
        verifyNoInteractions(prodRepo, productionEventPublisher);
    }

    @Test
    public void GivenAppointeeNotRegisteredUser_WhenAssignOwner_ThenReturnFalse() {
        // Arrange
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(productionEventPublisher.publishIsUserRegisteredEvent(APPOINTEE_ID)).thenReturn(false);

        // Act
        boolean result = productionService.assignOwner(VALID_TOKEN, COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertFalse(result);
        verifyNoInteractions(prodRepo);
    }

    @Test
    public void GivenValidToken_WhenAssignOwner_ThenCallerIdIsResolvedFromToken() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(productionEventPublisher.publishIsUserRegisteredEvent(APPOINTEE_ID)).thenReturn(true);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productionService.assignOwner(VALID_TOKEN, COMPANY_ID, APPOINTEE_ID);

        // Assert
        verify(authenticationService).getUser(VALID_TOKEN);
    }

    @Test
    public void GivenCompanyDoesNotExist_WhenAssignOwner_ThenReturnFalse() {
        // Arrange
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(productionEventPublisher.publishIsUserRegisteredEvent(APPOINTEE_ID)).thenReturn(true);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.empty());

        // Act
        boolean result = productionService.assignOwner(VALID_TOKEN, COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertFalse(result);
        verify(prodRepo, never()).save(any());
    }

    @Test
    public void GivenValidInput_WhenAssignOwner_ThenRepoSaveIsCalledWithUpdatedCompany() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(productionEventPublisher.publishIsUserRegisteredEvent(APPOINTEE_ID)).thenReturn(true);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        boolean result = productionService.assignOwner(VALID_TOKEN, COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertTrue(result);
        verify(prodRepo, times(1)).save(any());
        assertTrue(captor.getValue().isOwner(APPOINTEE_ID),
                "Repo must receive the company with the new owner already added");
    }

    @Test
    public void GivenFailedAssignment_WhenAssignOwner_ThenRepoSaveIsNeverCalled() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn("not-an-owner");
        when(productionEventPublisher.publishIsUserRegisteredEvent(APPOINTEE_ID)).thenReturn(true);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        // Act
        boolean result = productionService.assignOwner(VALID_TOKEN, COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertFalse(result);
        verify(prodRepo, never()).save(any());
    }

    @Test
    public void GivenFounderAsAppointerId_WhenAssignOwner_ThenReturnNonNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.assignOwner(
                FOUNDER_ID, COMPANY_ID, APPOINTEE_ID, company);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void GivenExistingOwnerAsAppointerId_WhenAssignOwner_ThenReturnNonNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.assignOwner(
                OWNER_ID, COMPANY_ID, APPOINTEE_ID, company);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void GivenCallerNotOwnerOfCompany_WhenAssignOwner_ThenReturnNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.assignOwner(
                "random-user", COMPANY_ID, APPOINTEE_ID, company);

        // Assert
        assertNull(result);
    }

    @Test
    public void GivenAppointeeAlreadyOwner_WhenAssignOwner_ThenReturnNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.assignOwner(
                FOUNDER_ID, COMPANY_ID, OWNER_ID, company);

        // Assert
        assertNull(result);
    }

    @Test
    public void GivenNullAppointeeId_WhenAssignOwner_ThenReturnNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.assignOwner(
                FOUNDER_ID, COMPANY_ID, null, company);

        // Assert
        assertNull(result);
    }

    @Test
    public void GivenFounderAppoints_WhenAssignOwner_ThenAppointeeHasFounderAsAppointer() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        productionHandler.assignOwner(FOUNDER_ID, COMPANY_ID, APPOINTEE_ID, company);

        // Assert
        OwnerDTO node = company.getOwnerDTO(APPOINTEE_ID).orElseThrow();
        assertEquals(FOUNDER_ID, node.getAppointerId());
    }

    @Test
    public void GivenOwnerAppoints_WhenAssignOwner_ThenAppointeeHasOwnerAsAppointer() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        productionHandler.assignOwner(OWNER_ID, COMPANY_ID, APPOINTEE_ID, company);

        // Assert
        OwnerDTO node = company.getOwnerDTO(APPOINTEE_ID).orElseThrow();
        assertEquals(OWNER_ID, node.getAppointerId());
    }

    @Test
    public void GivenSuccessfulAssignment_WhenAssignOwner_ThenReturnedCompanyContainsNewOwner() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.assignOwner(
                FOUNDER_ID, COMPANY_ID, APPOINTEE_ID, company);

        // Assert
        assertNotNull(result);
        assertTrue(result.isOwner(APPOINTEE_ID),
                "Returned company must contain the newly appointed owner");
    }

    @Test
    public void GivenFailedAssignment_WhenAssignOwner_ThenReturnNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.assignOwner(
                "not-an-owner", COMPANY_ID, APPOINTEE_ID, company);

        // Assert
        assertNull(result);
    }
}
