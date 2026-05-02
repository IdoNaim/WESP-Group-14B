package com.ticketpurchasingsystem.project.domain.ProductionTest;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Production.*;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateProductionCompanyTest {

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private IProdRepo prodRepo;

    private ProductionService productionService;
    private ProductionHandler productionHandler;

    private static final String VALID_TOKEN = "valid-token";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final String USER_ID = "eden-42";

    @BeforeEach
    void setUp() {
        productionHandler = new ProductionHandler();
        productionService = new ProductionService(authenticationService, productionHandler, prodRepo);
    }

    private ProductionCompanyDTO validDTO() {
        return new ProductionCompanyDTO("Awesome Events", "Great events company", "contact@awesome.com");
    }

    @Test
    public void GivenValidTokenAndCompanyDetails_WhenCreateProductionCompany_ThenReturnTrue() {
        // Arrange
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(prodRepo.findByName(any())).thenReturn(Optional.empty());
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        boolean result = productionService.createProductionCompany(VALID_TOKEN, validDTO());

        // Assert
        assertTrue(result);
    }

    @Test
    public void GivenInvalidToken_WhenCreateProductionCompany_ThenReturnFalse() {
        // Arrange
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        // Act
        boolean result = productionService.createProductionCompany(INVALID_TOKEN, validDTO());

        // Assert
        assertFalse(result);
        verifyNoInteractions(prodRepo);
    }

    @Test
    public void GivenValidToken_WhenCreateProductionCompany_ThenUserIdIsResolvedFromToken() {
        // Arrange
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(prodRepo.findByName(any())).thenReturn(Optional.empty());
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productionService.createProductionCompany(VALID_TOKEN, validDTO());

        // Assert
        verify(authenticationService).getUser(VALID_TOKEN);
        verify(prodRepo).save(argThat(c -> USER_ID.equals(c.getFounderId())));
    }

    @Test
    public void GivenCompanyNameAlreadyExists_WhenCreateProductionCompany_ThenReturnFalse() {
        // Arrange
        ProductionCompanyDTO dto = validDTO();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(prodRepo.findByName(dto.getCompanyName()))
                .thenReturn(Optional.of(new ProductionCompany(dto)));

        // Act
        boolean result = productionService.createProductionCompany(VALID_TOKEN, dto);

        // Assert
        assertFalse(result);
        verify(prodRepo, never()).save(any());
    }

    @Test
    public void GivenRepoThrowsException_WhenCreateProductionCompany_ThenReturnFalse() {
        // Arrange
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(prodRepo.findByName(any())).thenReturn(Optional.empty());
        when(prodRepo.save(any())).thenThrow(new RuntimeException("DB error"));

        // Act
        boolean result = productionService.createProductionCompany(VALID_TOKEN, validDTO());

        // Assert
        assertFalse(result);
    }

    @Test
    public void GivenValidInput_WhenCreateProductionCompany_ThenRepoSaveIsCalled() {
        // Arrange
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(prodRepo.findByName(any())).thenReturn(Optional.empty());
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productionService.createProductionCompany(VALID_TOKEN, validDTO());

        // Assert
        verify(prodRepo, times(1)).save(any(ProductionCompany.class));
    }

    @Test
    public void GivenBlankCompanyName_WhenCreateProductionCompany_ThenReturnNull() {
        // Arrange
        ProductionCompanyDTO dto = new ProductionCompanyDTO("   ", "desc", "email@test.com");

        // Act
        ProductionCompany result = productionHandler.createProductionCompany(USER_ID, dto);

        // Assert
        assertNull(result);
    }

    @Test
    public void GivenTwoDifferentFounders_WhenEachCreateOwnCompany_ThenEachCompanyHasCorrectFounder() {
        // Arrange
        String founderA = "founder-eden";
        String founderB = "founder-itay";
        ProductionCompanyDTO dtoA = new ProductionCompanyDTO("Eden Corp", "Eden's company", "eden@corp.com");
        ProductionCompanyDTO dtoB = new ProductionCompanyDTO("Itay Corp", "Itay's company", "itay@corp.com");

        // Act
        ProductionCompany companyA = productionHandler.createProductionCompany(founderA, dtoA);
        ProductionCompany companyB = productionHandler.createProductionCompany(founderB, dtoB);

        // Assert
        assertNotNull(companyA);
        assertNotNull(companyB);
        assertEquals(founderA, companyA.getFounderId());
        assertEquals(founderB, companyB.getFounderId());
    }

    @Test
    public void GivenFounderInitialisedTwice_WhenInitFounder_ThenFounderNotDuplicatedInOwners() {
        // Arrange
        ProductionCompany company = new ProductionCompany(validDTO());

        // Act
        company.initFounder(USER_ID);
        company.initFounder(USER_ID); // called a second time

        // Assert
        assertEquals(1, company.getOwnerIds().stream()
                .filter(id -> id.equals(USER_ID)).count(),
                "Founder ID must not be duplicated in ownerIds");
    }

    @Test
    public void GivenCompanyWithFounder_WhenAddOwner_ThenBothFounderAndOwnerAreInOwnerIds() {
        // Arrange
        ProductionCompany company = new ProductionCompany(validDTO());
        company.initFounder("founder-eden");

        // Act
        company.addOwnerId("owner-itay");

        // Assert
        assertTrue(company.getOwnerIds().contains("founder-eden"), "Founder must still be in ownerIds");
        assertTrue(company.getOwnerIds().contains("owner-itay"), "New owner must be in ownerIds");
        assertEquals(2, company.getOwnerIds().size());
    }
}