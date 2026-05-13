package com.ticketpurchasingsystem.project.domain.ProductionTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;

@ExtendWith(MockitoExtension.class)
public class GetCompanyPurchaseHistoryTest {

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
    private static final String OWNER_ID = "itay-1";
    private static final String NON_OWNER_ID = "stranger-99";
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
    public void GivenValidTokenAndFounder_WhenGetCompanyPurchaseHistory_ThenReturnHistory() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        List<String> seats = List.of("seat1");
        HashMap<String,Integer> standing = new HashMap<>();
        standing.put("area1", 2);
        List<HistoryOrderItem> mockHistory = List.of(
                new HistoryOrderItem("o1", FOUNDER_ID, "e1", COMPANY_ID, 10.0, seats, standing));
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(productionEventPublisher.publishGetCompanyHistoryEvent(COMPANY_ID)).thenReturn(mockHistory);

        // Act
        List<HistoryOrderItem> result = productionService.getCompanyPurchaseHistory(VALID_TOKEN, COMPANY_ID);

        // Assert
        assertNotNull(result);
        assertEquals(mockHistory, result);
    }

    @Test
    public void GivenValidTokenAndOwner_WhenGetCompanyPurchaseHistory_ThenReturnHistory() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        List<String> seats = List.of("seat1");
        HashMap<String,Integer> standing = new HashMap<>();
        standing.put("area1", 2);
        List<HistoryOrderItem> mockHistory = List.of(
                new HistoryOrderItem("o2", OWNER_ID, "e2", COMPANY_ID, 10.0, seats, standing));
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(OWNER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(productionEventPublisher.publishGetCompanyHistoryEvent(COMPANY_ID)).thenReturn(mockHistory);

        // Act
        List<HistoryOrderItem> result = productionService.getCompanyPurchaseHistory(VALID_TOKEN, COMPANY_ID);

        // Assert
        assertNotNull(result);
        assertEquals(mockHistory, result);
    }

    @Test
    public void GivenInvalidToken_WhenGetCompanyPurchaseHistory_ThenReturnNull() {
        // Arrange
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        // Act
        List<HistoryOrderItem> result = productionService.getCompanyPurchaseHistory(INVALID_TOKEN, COMPANY_ID);

        // Assert
        assertNull(result);
        verifyNoInteractions(prodRepo, productionEventPublisher);
    }

    @Test
    public void GivenValidTokenButNotOwner_WhenGetCompanyPurchaseHistory_ThenReturnNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(NON_OWNER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        // Act
        List<HistoryOrderItem> result = productionService.getCompanyPurchaseHistory(VALID_TOKEN, COMPANY_ID);

        // Assert
        assertNull(result);
        verifyNoInteractions(productionEventPublisher);
    }

    @Test
    public void GivenCompanyNotFound_WhenGetCompanyPurchaseHistory_ThenReturnNull() {
        // Arrange
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.empty());

        // Act
        List<HistoryOrderItem> result = productionService.getCompanyPurchaseHistory(VALID_TOKEN, COMPANY_ID);

        // Assert
        assertNull(result);
        verifyNoInteractions(productionEventPublisher);
    }

    @Test
    public void GivenNoPurchaseData_WhenGetCompanyPurchaseHistory_ThenReturnEmptyList() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(productionEventPublisher.publishGetCompanyHistoryEvent(COMPANY_ID))
                .thenReturn(Collections.emptyList());

        // Act
        List<HistoryOrderItem> result = productionService.getCompanyPurchaseHistory(VALID_TOKEN, COMPANY_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void GivenPublisherReturnsNull_WhenGetCompanyPurchaseHistory_ThenReturnEmptyList() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(productionEventPublisher.publishGetCompanyHistoryEvent(COMPANY_ID)).thenReturn(null);

        // Act
        List<HistoryOrderItem> result = productionService.getCompanyPurchaseHistory(VALID_TOKEN, COMPANY_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void GivenValidInput_WhenGetCompanyPurchaseHistory_ThenPublisherCalledWithCorrectCompanyId() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(productionEventPublisher.publishGetCompanyHistoryEvent(anyInt()))
                .thenReturn(Collections.emptyList());

        // Act
        productionService.getCompanyPurchaseHistory(VALID_TOKEN, COMPANY_ID);

        // Assert
        verify(productionEventPublisher, times(1)).publishGetCompanyHistoryEvent(COMPANY_ID);
    }

    @Test
    public void GivenValidInput_WhenGetCompanyPurchaseHistory_ThenUserIdResolvedFromToken() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(productionEventPublisher.publishGetCompanyHistoryEvent(anyInt()))
                .thenReturn(Collections.emptyList());

        // Act
        productionService.getCompanyPurchaseHistory(VALID_TOKEN, COMPANY_ID);

        // Assert
        verify(authenticationService).getUser(VALID_TOKEN);
    }
}
