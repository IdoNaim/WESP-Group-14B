package com.ticketpurchasingsystem.project.domain.ProductionTest;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Production.*;
import com.ticketpurchasingsystem.project.application.UserService.IUserService;
import com.ticketpurchasingsystem.project.domain.User.UserDTO;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.AssignOwnerEvent;

import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

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
    private ProdPublisher publisher;
    @Mock
    private IUserService userService;

    private ProductionHandler productionHandler;
    private ProductionService productionService;

    private static final String VALID_TOKEN = "valid-token";
    private static final String INVALID_TOKEN = "bad-token";
    private static final String FOUNDER_ID = "founder-1";
    private static final String OWNER_ID = "owner-1";
    private static final String APPOINTEE_ID = "new-user-99";
    private static final Integer COMPANY_ID = 1;

    @BeforeEach
    void setUp() {
        productionHandler = new ProductionHandler(prodRepo, publisher);
        productionService = new ProductionService(authenticationService, productionHandler, userService);
    }

    private ProductionCompany companyWithFounderAndOwner() {
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Test Co", "desc", "test@co.com");
        ProductionCompany company = new ProductionCompany(dto);
        company.setCompanyId(COMPANY_ID);
        company.initFounder(FOUNDER_ID);
        company.appointOwner(FOUNDER_ID, OWNER_ID);
        return company;
    }

    private UserDTO fakeUserDTO(String userId) {
        return new UserDTO(userId, "name", "email@test.com", null);
    }

    @Test
    public void GivenValidTokenAndValidAppointee_WhenAssignOwner_ThenReturnTrue() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(userService.getUser(APPOINTEE_ID)).thenReturn(fakeUserDTO(APPOINTEE_ID));
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
        verifyNoInteractions(prodRepo, userService, publisher);
    }

    @Test
    public void GivenAppointeeNotRegisteredUser_WhenAssignOwner_ThenReturnFalse() {
        // Arrange
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(userService.getUser(APPOINTEE_ID)).thenReturn(null); // not registered

        // Act
        boolean result = productionService.assignOwner(VALID_TOKEN, COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertFalse(result);
        verifyNoInteractions(prodRepo, publisher);
    }

    @Test
    public void GivenValidToken_WhenAssignOwner_ThenCallerIdIsResolvedFromToken() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(userService.getUser(APPOINTEE_ID)).thenReturn(fakeUserDTO(APPOINTEE_ID));
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productionService.assignOwner(VALID_TOKEN, COMPANY_ID, APPOINTEE_ID);

        // Assert
        verify(authenticationService).getUser(VALID_TOKEN);
    }

    @Test
    public void GivenFounderAsAppointerId_WhenAssignOwner_ThenReturnTrue() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        boolean result = productionHandler.assignOwner(FOUNDER_ID, COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertTrue(result);
    }

    @Test
    public void GivenExistingOwnerAsAppointerId_WhenAssignOwner_ThenReturnTrue() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        boolean result = productionHandler.assignOwner(OWNER_ID, COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertTrue(result);
    }

    @Test
    public void GivenCallerNotOwnerOfCompany_WhenAssignOwner_ThenReturnFalse() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        // Act
        boolean result = productionHandler.assignOwner("random-user", COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertFalse(result);
        verify(prodRepo, never()).save(any());
    }

    @Test
    public void GivenAppointeeAlreadyOwner_WhenAssignOwner_ThenReturnFalse() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        // Act
        boolean result = productionHandler.assignOwner(FOUNDER_ID, COMPANY_ID, OWNER_ID);

        // Assert
        assertFalse(result);
        verify(prodRepo, never()).save(any());
    }

    @Test
    public void GivenCompanyDoesNotExist_WhenAssignOwner_ThenReturnFalse() {
        // Arrange
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.empty());

        // Act
        boolean result = productionHandler.assignOwner(FOUNDER_ID, COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertFalse(result);
        verify(prodRepo, never()).save(any());
    }

    @Test
    public void GivenNullAppointeeId_WhenAssignOwner_ThenReturnFalse() {
        assertFalse(productionHandler.assignOwner(FOUNDER_ID, COMPANY_ID, null));
        verifyNoInteractions(prodRepo, publisher);
    }

    @Test
    public void GivenValidInput_WhenAssignOwner_ThenAssignOwnerEventIsPublished() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productionHandler.assignOwner(FOUNDER_ID, COMPANY_ID, APPOINTEE_ID);

        // Assert
        verify(publisher, times(1)).publish(any(AssignOwnerEvent.class));
    }

    @Test
    public void GivenFailedAssignment_WhenAssignOwner_ThenNoEventIsPublished() {
        // Arrange — caller is not an owner so assignment will fail
        ProductionCompany company = companyWithFounderAndOwner();
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        // Act
        productionHandler.assignOwner("not-an-owner", COMPANY_ID, APPOINTEE_ID);

        // Assert
        verify(publisher, never()).publish(any(AssignOwnerEvent.class));
    }

    @Test
    public void GivenFounderAppoints_WhenAssignOwner_ThenAppointeeHasFounderAsAppointer() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productionHandler.assignOwner(FOUNDER_ID, COMPANY_ID, APPOINTEE_ID);

        // Assert
        OwnerDTO node = company.getOwnerDTO(APPOINTEE_ID).orElseThrow();
        assertEquals(FOUNDER_ID, node.getAppointerId());
    }

    @Test
    public void GivenOwnerAppoints_WhenAssignOwner_ThenAppointeeHasOwnerAsAppointer() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productionHandler.assignOwner(OWNER_ID, COMPANY_ID, APPOINTEE_ID);

        // Assert
        OwnerDTO node = company.getOwnerDTO(APPOINTEE_ID).orElseThrow();
        assertEquals(OWNER_ID, node.getAppointerId());
    }

    @Test
    public void GivenSuccessfulAssignment_WhenAssignOwner_ThenRepoSaveIsCalledWithUpdatedCompany() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        // need to capture the company that was saved
        ArgumentCaptor<ProductionCompany> captor = ArgumentCaptor.forClass(ProductionCompany.class);

        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        boolean result = productionHandler.assignOwner(FOUNDER_ID, COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertTrue(result);
        verify(prodRepo, times(1)).save(any(ProductionCompany.class));
        ProductionCompany saved = captor.getValue();
        assertTrue(saved.isOwner(APPOINTEE_ID),
                "Repo must receive the company with the new owner already added");
    }

    @Test
    public void GivenFailedAssignment_WhenAssignOwner_ThenRepoSaveIsNeverCalled() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        // Act
        boolean result = productionHandler.assignOwner("not-an-owner", COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertFalse(result);
        verify(prodRepo, never()).save(any());
    }

}