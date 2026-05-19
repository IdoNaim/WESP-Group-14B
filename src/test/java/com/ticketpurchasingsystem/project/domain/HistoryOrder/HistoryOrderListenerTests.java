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
import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllHistoryOrdersEvent;

@ExtendWith(MockitoExtension.class)
class HistoryOrderListenerTests {

    @Mock
    private IHistoryOrderRepo historyOrderRepo;

    @Mock
    private IHistoryOrderService historyOrderService;

    private HistoryOrderListener listener;
    private static final String REQ_ID = "admin-test";

    @BeforeEach
    void setUp() {
        listener = new HistoryOrderListener(historyOrderRepo, historyOrderService);
    }

    @Test
    void WhenHandleGetAllHistoryOrdersEventGivenOrders_ThenEventResultIsSet() {

        List<HistoryOrderItem> mockHistory = List.of(new HistoryOrderItem("1", "user", "event", 1, 10.0, new ArrayList<>(), new HashMap<>()));
        when(historyOrderRepo.findAll()).thenReturn(mockHistory);
        GetAllHistoryOrdersEvent event = new GetAllHistoryOrdersEvent(REQ_ID);

        listener.onApplicationEvent(event);

        assertEquals(mockHistory, event.getResult());
    }

    @Test
    void WhenHandleGetAllHistoryOrdersEventGivenEmptyRepo_ThenEventResultIsEmpty() {
        when(historyOrderRepo.findAll()).thenReturn(Collections.emptyList());
        GetAllHistoryOrdersEvent event = new GetAllHistoryOrdersEvent(REQ_ID);

        listener.onApplicationEvent(event);

        assertTrue(event.getResult().isEmpty());
    }

    @Test
    void WhenHandleGetAllHistoryOrdersEventGiven_ThenRepoCalledOnce() {
        when(historyOrderRepo.findAll()).thenReturn(Collections.emptyList());
        GetAllHistoryOrdersEvent event = new GetAllHistoryOrdersEvent(REQ_ID);

        listener.onApplicationEvent(event);

        verify(historyOrderRepo, times(1)).findAll();
    }

    @Test
    void WhenHandleGetAllHistoryOrdersEventGivenReqId_ThenEventPreservesReqId() {
        when(historyOrderRepo.findAll()).thenReturn(Collections.emptyList());
        GetAllHistoryOrdersEvent event = new GetAllHistoryOrdersEvent(REQ_ID);

        listener.onApplicationEvent(event);

        assertEquals(REQ_ID, event.getReqId());
    }
}
