package com.ticketpurchasingsystem.project.domain.ProductionTest;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Production.*;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.NewProdEvent;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.infrastructure.ProdRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateProductionCompanyTest {

    // mocks
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private IProdRepo prodRepo;
    @Mock
    private ProdPublisher publisher;

    private ProductionService productionService;
    private ProductionHandler productionHandler;

    private static final String VALID_TOKEN = "valid-token";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final String USER_ID = "user-42";

    @BeforeEach
    void setUp() {
        productionHandler = new ProductionHandler(prodRepo, publisher);
        productionService = new ProductionService(authenticationService, productionHandler);
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
        verifyNoInteractions(prodRepo, publisher);
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
    public void GivenValidUserAndCompanyDetails_WhenCreateProductionCompany_ThenReturnTrue() {
        // Arrange
        when(prodRepo.findByName(any())).thenReturn(Optional.empty());
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        boolean result = productionHandler.createProductionCompany(USER_ID, validDTO());

        // Assert
        assertTrue(result);
    }

    @Test
    public void GivenBlankCompanyName_WhenCreateProductionCompany_ThenReturnFalse() {
        // Arrange
        ProductionCompanyDTO dto = new ProductionCompanyDTO("   ", "desc", "email@test.com");

        // Act
        boolean result = productionHandler.createProductionCompany(USER_ID, dto);

        // Assert
        assertFalse(result);
        verifyNoInteractions(prodRepo, publisher);
    }

    @Test
    public void GivenCompanyNameAlreadyExists_WhenCreateProductionCompany_ThenReturnFalse() {
        // Arrange
        ProductionCompanyDTO dto = validDTO();
        when(prodRepo.findByName(dto.getCompanyName())).thenReturn(Optional.of(new ProductionCompany(dto)));

        // Act
        boolean result = productionHandler.createProductionCompany(USER_ID, dto);

        // Assert
        assertFalse(result);
        verify(prodRepo, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    public void GivenValidInput_WhenCreateProductionCompany_ThenEventIsPublished() {
        // Arrange
        when(prodRepo.findByName(any())).thenReturn(Optional.empty());
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productionHandler.createProductionCompany(USER_ID, validDTO());

        // Assert
        verify(publisher, times(1)).publish(any(NewProdEvent.class));
    }

    @Test
    public void GivenRepoThrowsException_WhenCreateProductionCompany_ThenReturnFalse() {
        // Arrange
        when(prodRepo.findByName(any())).thenReturn(Optional.empty());
        when(prodRepo.save(any())).thenThrow(new RuntimeException("DB error"));

        // Act
        boolean result = productionHandler.createProductionCompany(USER_ID, validDTO());

        // Assert
        assertFalse(result);
        verifyNoInteractions(publisher);
    }

    @Test
    public void GivenValidInput_WhenCreateProductionCompany_ThenFounderIdIsSet() {
        // Arrange
        ArgumentCaptor<ProductionCompany> captor = ArgumentCaptor.forClass(ProductionCompany.class);
        when(prodRepo.findByName(any())).thenReturn(Optional.empty());
        when(prodRepo.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productionHandler.createProductionCompany(USER_ID, validDTO());

        // Assert
        assertEquals(USER_ID, captor.getValue().getFounderId(),
                "Founder ID must match the userId who requested company creation");
    }

    @Test
    public void GivenValidInput_WhenCreateProductionCompany_ThenFounderIsAlsoInOwnerIds() {
        // Arrange
        ArgumentCaptor<ProductionCompany> captor = ArgumentCaptor.forClass(ProductionCompany.class);
        when(prodRepo.findByName(any())).thenReturn(Optional.empty());
        when(prodRepo.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productionHandler.createProductionCompany(USER_ID, validDTO());

        // Assert
        assertTrue(captor.getValue().getOwnerIds().contains(USER_ID),
                "Founder must automatically appear in ownerIds");
    }

    @Test
    public void GivenValidInput_WhenCreateProductionCompany_ThenOwnerIdsHasExactlyOneEntry() {
        // Arrange
        ArgumentCaptor<ProductionCompany> captor = ArgumentCaptor.forClass(ProductionCompany.class);
        when(prodRepo.findByName(any())).thenReturn(Optional.empty());
        when(prodRepo.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productionHandler.createProductionCompany(USER_ID, validDTO());

        // Assert — no extra owners should be added automatically
        assertEquals(1, captor.getValue().getOwnerIds().size(),
                "A brand-new company should have exactly one owner (the founder)");
    }

    @Test
    public void GivenTwoDifferentFounders_WhenEachCreateOwnCompany_ThenEachCompanyHasCorrectFounder() {
        // Arrange
        String founderA = "founder-alice";
        String founderB = "founder-bob";
        ProductionCompanyDTO dtoA = new ProductionCompanyDTO("Alice Corp", "Alice's company", "alice@corp.com");
        ProductionCompanyDTO dtoB = new ProductionCompanyDTO("Bob Corp", "Bob's company", "bob@corp.com");

        ArgumentCaptor<ProductionCompany> captor = ArgumentCaptor.forClass(ProductionCompany.class);
        when(prodRepo.findByName(any())).thenReturn(Optional.empty());
        when(prodRepo.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productionHandler.createProductionCompany(founderA, dtoA);
        productionHandler.createProductionCompany(founderB, dtoB);

        // Assert
        List<ProductionCompany> saved = captor.getAllValues();
        assertEquals(founderA, saved.get(0).getFounderId());
        assertEquals(founderB, saved.get(1).getFounderId());
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
        company.initFounder("founder-1");

        // Act
        company.addOwnerId("owner-2");

        // Assert
        assertTrue(company.getOwnerIds().contains("founder-1"), "Founder must still be in ownerIds");
        assertTrue(company.getOwnerIds().contains("owner-2"), "New owner must be in ownerIds");
        assertEquals(2, company.getOwnerIds().size());
    }

}