package com.ticketpurchasingsystem.project.domain.ActiveOrders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllActiveOrdersEvent;

class ActiveOrderListenerTests {

    private IActiveOrderRepo activeOrderRepo;
    private ActiveOrderListener listener;
    private static final String REQ_ID = "admin-test";

    @BeforeEach
    void setUp() {
        activeOrderRepo = mock(IActiveOrderRepo.class);
        listener = new ActiveOrderListener(activeOrderRepo);
    }

    @Test
    void WhenHandleGetAllActiveOrdersEventGivenOrders_ThenEventResultIsSet() {
        List<ActiveOrderItem> mockOrders = List.of(new ActiveOrderItem("1", "1","1",1));
        when(activeOrderRepo.findAll()).thenReturn(mockOrders);
        GetAllActiveOrdersEvent event = new GetAllActiveOrdersEvent(REQ_ID);

        listener.handleGetAllActiveOrdersEvent(event);

        assertEquals(mockOrders, event.getResult());
    }

    @Test
    void WhenHandleGetAllActiveOrdersEventGivenEmptyRepo_ThenEventResultIsEmpty() {
        when(activeOrderRepo.findAll()).thenReturn(Collections.emptyList());
        GetAllActiveOrdersEvent event = new GetAllActiveOrdersEvent(REQ_ID);

        listener.handleGetAllActiveOrdersEvent(event);

        assertTrue(event.getResult().isEmpty());
    }


    @Test
    void WhenHandleGetAllActiveOrdersEventGiven_ThenRepoCalledOnce() {
        when(activeOrderRepo.findAll()).thenReturn(Collections.emptyList());
        GetAllActiveOrdersEvent event = new GetAllActiveOrdersEvent(REQ_ID);

        listener.handleGetAllActiveOrdersEvent(event);

        verify(activeOrderRepo, times(1)).findAll();
    }

    @Test
    void WhenHandleGetAllActiveOrdersEventGivenReqId_ThenEventPreservesReqId() {
        when(activeOrderRepo.findAll()).thenReturn(Collections.emptyList());
        GetAllActiveOrdersEvent event = new GetAllActiveOrdersEvent(REQ_ID);

        listener.handleGetAllActiveOrdersEvent(event);

        assertEquals(REQ_ID, event.getReqId());
    }
}
