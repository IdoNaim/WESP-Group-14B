package com.ticketpurchasingsystem.project.domain.ProductionTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.GetCompanyHistoryEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.IsUserRegisteredEvent;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class ProductionEventPublisherTests {

    @Mock
    private ApplicationEventPublisher springPublisher;
    private ProductionEventPublisher productionEventPublisher;

    private static final String USER_ID = "user-42";
    private static final int COMPANY_ID = 7;

    @BeforeEach
    void setUp() {
        productionEventPublisher = new ProductionEventPublisher(springPublisher);
    }

    // --- publishIsUserRegisteredEvent ---

    @Test
    void WhenPublishIsUserRegisteredGivenListenerSetsTrue_ThenReturnTrue() {
        // Arrange
        doAnswer(inv -> {
            IsUserRegisteredEvent event = inv.getArgument(0);
            event.setRegistered(true);
            return null;
        }).when(springPublisher).publishEvent((Object) any());

        // Act
        boolean result = productionEventPublisher.publishIsUserRegisteredEvent(USER_ID);

        // Assert
        assertTrue(result);
    }

    @Test
    void WhenPublishIsUserRegisteredGivenNoListener_ThenReturnFalse() {
        // Act
        boolean result = productionEventPublisher.publishIsUserRegisteredEvent(USER_ID);

        // Assert
        assertFalse(result);
    }

    @Test
    void WhenPublishIsUserRegisteredGivenUserId_ThenEventCarriesUserId() {
        // Arrange
        doAnswer(inv -> {
            IsUserRegisteredEvent event = inv.getArgument(0);
            assertEquals(USER_ID, event.getUserId());
            return null;
        }).when(springPublisher).publishEvent((Object) any());

        // Act
        productionEventPublisher.publishIsUserRegisteredEvent(USER_ID);

        // Assert
        verify(springPublisher, times(1)).publishEvent((Object) any());
    }

    @Test
    void WhenPublishIsUserRegisteredGivenListenerSetsFalse_ThenReturnFalse() {
        // Arrange
        doAnswer(inv -> {
            IsUserRegisteredEvent event = inv.getArgument(0);
            event.setRegistered(false);
            return null;
        }).when(springPublisher).publishEvent((Object) any());

        // Act
        boolean result = productionEventPublisher.publishIsUserRegisteredEvent(USER_ID);

        // Assert
        assertFalse(result);
    }

    // --- publishGetCompanyHistoryEvent ---

    @Test
    void WhenPublishGetCompanyHistoryGivenListenerResponds_ThenReturnHistory() {
        // Arrange
        List<String> seats = List.of("seat1");
        HashMap<String,Integer> standing = new HashMap<>();
        standing.put("area1", 2);
        List<HistoryOrderItem> mockHistory = List.of(
                new HistoryOrderItem("o1", USER_ID, "e1", 10.0, seats, standing));
        doAnswer(inv -> {
            GetCompanyHistoryEvent event = inv.getArgument(0);
            event.setResult(mockHistory);
            return null;
        }).when(springPublisher).publishEvent((Object) any());

        // Act
        List<HistoryOrderItem> result = productionEventPublisher.publishGetCompanyHistoryEvent(COMPANY_ID);

        // Assert
        assertEquals(mockHistory, result);
    }

    @Test
    void WhenPublishGetCompanyHistoryGivenNoListener_ThenReturnNull() {
        // Act
        List<HistoryOrderItem> result = productionEventPublisher.publishGetCompanyHistoryEvent(COMPANY_ID);

        // Assert
        assertNull(result);
    }

    @Test
    void WhenPublishGetCompanyHistoryGivenEmptyResult_ThenReturnEmptyList() {
        // Arrange
        doAnswer(inv -> {
            GetCompanyHistoryEvent event = inv.getArgument(0);
            event.setResult(Collections.emptyList());
            return null;
        }).when(springPublisher).publishEvent((Object) any());

        // Act
        List<HistoryOrderItem> result = productionEventPublisher.publishGetCompanyHistoryEvent(COMPANY_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void WhenPublishGetCompanyHistoryGivenCompanyId_ThenEventCarriesCompanyId() {
        // Arrange
        doAnswer(inv -> {
            GetCompanyHistoryEvent event = inv.getArgument(0);
            assertEquals(COMPANY_ID, event.getCompanyId());
            event.setResult(Collections.emptyList());
            return null;
        }).when(springPublisher).publishEvent((Object) any());

        // Act
        productionEventPublisher.publishGetCompanyHistoryEvent(COMPANY_ID);

        // Assert
        verify(springPublisher, times(1)).publishEvent((Object) any());
    }
}
