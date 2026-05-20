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
    void GivenOrdersExistInRepo_WhenOnGetCompanyHistory_ThenEventResultIsSet() {
        List<String> seats = List.of("seat1");
        HashMap<String,Integer> standing = new HashMap<>();
        standing.put("area1", 2);
        List<HistoryOrderItem> mockHistory = List.of(
                new HistoryOrderItem("o1", "user-1", "e1", COMPANY_ID, 10.0, seats, standing));
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
