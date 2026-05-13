package com.ticketpurchasingsystem.project.domain.HistoryOrder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ticketpurchasingsystem.project.application.IHistoryOrderService;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.GetCompanyHistoryEvent;

@ExtendWith(MockitoExtension.class)
class HistoryOrderListenerCompanyHistoryTests {

    @Mock
    private IHistoryOrderRepo historyOrderRepo;

    @Mock
    private IHistoryOrderService historyOrderService;

    private HistoryOrderListener listener;

    private static final int COMPANY_ID = 42;

    @BeforeEach
    void setUp() {
        listener = new HistoryOrderListener(historyOrderRepo, historyOrderService);
    }

    @Test
    void WhenOnGetCompanyHistoryGivenOrders_ThenEventResultIsSet() {
        // Arrange
        List<String> seats = List.of("seat1");
        HashMap<String,Integer> standing = new HashMap<>();
        standing.put("area1", 2);
        List<HistoryOrderItem> mockHistory = List.of(
                new HistoryOrderItem("o1", "user-1", "e1", COMPANY_ID, 10.0, seats, standing));
        when(historyOrderRepo.findAllByCompanyId(COMPANY_ID)).thenReturn(mockHistory);
        GetCompanyHistoryEvent event = new GetCompanyHistoryEvent(COMPANY_ID);

        // Act
        listener.onGetCompanyHistory(event);

        // Assert
        assertEquals(mockHistory, event.getResult());
    }

    @Test
    void WhenOnGetCompanyHistoryGivenEmptyRepo_ThenEventResultIsEmpty() {
        // Arrange
        when(historyOrderRepo.findAllByCompanyId(COMPANY_ID)).thenReturn(Collections.emptyList());
        GetCompanyHistoryEvent event = new GetCompanyHistoryEvent(COMPANY_ID);

        // Act
        listener.onGetCompanyHistory(event);

        // Assert
        assertTrue(event.getResult().isEmpty());
    }

    @Test
    void WhenOnGetCompanyHistoryGivenCompanyId_ThenRepoCalledOnceWithCorrectId() {
        // Arrange
        when(historyOrderRepo.findAllByCompanyId(COMPANY_ID)).thenReturn(Collections.emptyList());
        GetCompanyHistoryEvent event = new GetCompanyHistoryEvent(COMPANY_ID);

        // Act
        listener.onGetCompanyHistory(event);

        // Assert
        verify(historyOrderRepo, times(1)).findAllByCompanyId(COMPANY_ID);
    }

    @Test
    void WhenOnGetCompanyHistoryGivenCompanyId_ThenFindAllIsNeverCalled() {
        // Arrange
        when(historyOrderRepo.findAllByCompanyId(COMPANY_ID)).thenReturn(Collections.emptyList());
        GetCompanyHistoryEvent event = new GetCompanyHistoryEvent(COMPANY_ID);

        // Act
        listener.onGetCompanyHistory(event);

        // Assert
        verify(historyOrderRepo, never()).findAll();
    }

    @Test
    void WhenOnGetCompanyHistoryGivenCompanyId_ThenEventPreservesCompanyId() {
        // Arrange
        when(historyOrderRepo.findAllByCompanyId(COMPANY_ID)).thenReturn(Collections.emptyList());
        GetCompanyHistoryEvent event = new GetCompanyHistoryEvent(COMPANY_ID);

        // Act
        listener.onGetCompanyHistory(event);

        // Assert
        assertEquals(COMPANY_ID, event.getCompanyId());
    }
}
