package com.ticketpurchasingsystem.project.domain.HistoryOrder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ticketpurchasingsystem.project.application.IHistoryOrderService;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.GetCompanyHistoryEvent;

@ExtendWith(MockitoExtension.class)
class HistoryOrderListenerCompanyHistoryTests {

    @Mock private IHistoryOrderRepo historyOrderRepo;
    @Mock private IHistoryOrderService historyOrderService;

    private HistoryOrderListener listener;

    private static final int COMPANY_ID = 42;

    @BeforeEach
    void setUp() {
        listener = new HistoryOrderListener(historyOrderRepo, historyOrderService);
    }

    private HistoryOrderItem buildItem() {
        HashMap<String, Integer> standing = new HashMap<>();
        standing.put("area1", 2);
        return new HistoryOrderItem("o1", "user-1", "e1", COMPANY_ID, 10.0, List.of("seat1"), standing);
    }

    @Test
    void GivenOrdersExistInRepo_WhenOnGetCompanyHistory_ThenEventResultIsSet() {
        List<HistoryOrderItem> mockHistory = List.of(buildItem());
        when(historyOrderRepo.findAllByCompanyId(COMPANY_ID)).thenReturn(mockHistory);
        GetCompanyHistoryEvent event = new GetCompanyHistoryEvent(COMPANY_ID);

        listener.onGetCompanyHistory(event);

        assertEquals(mockHistory, event.getResult());
    }

    @Test
    void GivenEmptyRepo_WhenOnGetCompanyHistory_ThenEventResultIsEmpty() {
        when(historyOrderRepo.findAllByCompanyId(COMPANY_ID)).thenReturn(Collections.emptyList());
        GetCompanyHistoryEvent event = new GetCompanyHistoryEvent(COMPANY_ID);

        listener.onGetCompanyHistory(event);

        assertTrue(event.getResult().isEmpty());
    }

    @Test
    void GivenValidCompanyId_WhenOnGetCompanyHistory_ThenRepoCalledOnceWithCorrectId() {
        when(historyOrderRepo.findAllByCompanyId(COMPANY_ID)).thenReturn(Collections.emptyList());
        GetCompanyHistoryEvent event = new GetCompanyHistoryEvent(COMPANY_ID);

        listener.onGetCompanyHistory(event);

        verify(historyOrderRepo, times(1)).findAllByCompanyId(COMPANY_ID);
    }

    @Test
    void GivenValidCompanyId_WhenOnGetCompanyHistory_ThenFindAllIsNeverCalled() {
        when(historyOrderRepo.findAllByCompanyId(COMPANY_ID)).thenReturn(Collections.emptyList());
        GetCompanyHistoryEvent event = new GetCompanyHistoryEvent(COMPANY_ID);

        listener.onGetCompanyHistory(event);

        verify(historyOrderRepo, never()).findAll();
    }

    @Test
    void GivenValidCompanyId_WhenOnGetCompanyHistory_ThenEventPreservesCompanyId() {
        when(historyOrderRepo.findAllByCompanyId(COMPANY_ID)).thenReturn(Collections.emptyList());
        GetCompanyHistoryEvent event = new GetCompanyHistoryEvent(COMPANY_ID);

        listener.onGetCompanyHistory(event);

        assertEquals(COMPANY_ID, event.getCompanyId());
    }
}
