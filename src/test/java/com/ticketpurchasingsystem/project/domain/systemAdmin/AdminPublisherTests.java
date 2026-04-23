package com.ticketpurchasingsystem.project.domain.systemAdmin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllActiveOrdersEvent;
import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllHistoryOrdersEvent;

class AdminPublisherTests {

    private ApplicationEventPublisher springPublisher;
    private AdminPublisher adminPublisher;
    private static final String ADMIN_ID = "admin-test";

    @BeforeEach
    void setUp() {
        springPublisher = mock(ApplicationEventPublisher.class);
        adminPublisher = new AdminPublisher(springPublisher);
    }

    // --- publishGetAllActiveOrders ---

    @Test
    void WhenPublishGetAllActiveOrdersGivenListenerResponds_ThenReturnOrders() {
        List<ActiveOrderItem> mockOrders = List.of(new ActiveOrderItem());
        doAnswer(invocation -> {
            GetAllActiveOrdersEvent event = invocation.getArgument(0, GetAllActiveOrdersEvent.class);
            event.setResult(mockOrders);
            return null;
        }).when(springPublisher).publishEvent(any(GetAllActiveOrdersEvent.class));

        List<ActiveOrderItem> result = adminPublisher.publishGetAllActiveOrders(ADMIN_ID);

        assertEquals(mockOrders, result);
    }

    @Test
    void WhenPublishGetAllActiveOrdersGivenNoListener_ThenReturnNull() {
        List<ActiveOrderItem> result = adminPublisher.publishGetAllActiveOrders(ADMIN_ID);

        assertNull(result);
    }

    @Test
    void WhenPublishGetAllActiveOrdersGivenAdminId_ThenEventCarriesAdminId() {
        doAnswer(invocation -> {
            GetAllActiveOrdersEvent event = invocation.getArgument(0, GetAllActiveOrdersEvent.class);
            assertEquals(ADMIN_ID, event.getReqId());
            return null;
        }).when(springPublisher).publishEvent(any(GetAllActiveOrdersEvent.class));

        adminPublisher.publishGetAllActiveOrders(ADMIN_ID);

        verify(springPublisher).publishEvent(any(GetAllActiveOrdersEvent.class));
    }

    @Test
    void WhenPublishGetAllActiveOrdersGivenEmptyList_ThenReturnEmptyList() {
        doAnswer(invocation -> {
            GetAllActiveOrdersEvent event = invocation.getArgument(0, GetAllActiveOrdersEvent.class);
            event.setResult(Collections.emptyList());
            return null;
        }).when(springPublisher).publishEvent(any(GetAllActiveOrdersEvent.class));

        List<ActiveOrderItem> result = adminPublisher.publishGetAllActiveOrders(ADMIN_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // --- publishGetAllOrdersHistory ---

    @Test
    void WhenPublishGetAllOrdersHistoryGivenListenerResponds_ThenReturnHistory() {
        List<HistoryOrderItem> mockHistory = List.of(new HistoryOrderItem());
        doAnswer(invocation -> {
            GetAllHistoryOrdersEvent event = invocation.getArgument(0, GetAllHistoryOrdersEvent.class);
            event.setResult(mockHistory);
            return null;
        }).when(springPublisher).publishEvent(any(GetAllHistoryOrdersEvent.class));

        List<HistoryOrderItem> result = adminPublisher.publishGetAllOrdersHistory(ADMIN_ID);

        assertEquals(mockHistory, result);
    }

    @Test
    void WhenPublishGetAllOrdersHistoryGivenNoListener_ThenReturnNull() {
        List<HistoryOrderItem> result = adminPublisher.publishGetAllOrdersHistory(ADMIN_ID);

        assertNull(result);
    }

    @Test
    void WhenPublishGetAllOrdersHistoryGivenAdminId_ThenEventCarriesAdminId() {
        doAnswer(invocation -> {
            GetAllHistoryOrdersEvent event = invocation.getArgument(0, GetAllHistoryOrdersEvent.class);
            assertEquals(ADMIN_ID, event.getReqId());
            return null;
        }).when(springPublisher).publishEvent(any(GetAllHistoryOrdersEvent.class));

        adminPublisher.publishGetAllOrdersHistory(ADMIN_ID);

        verify(springPublisher).publishEvent(any(GetAllHistoryOrdersEvent.class));
    }

    @Test
    void WhenPublishGetAllOrdersHistoryGivenEmptyList_ThenReturnEmptyList() {
        doAnswer(invocation -> {
            GetAllHistoryOrdersEvent event = invocation.getArgument(0, GetAllHistoryOrdersEvent.class);
            event.setResult(Collections.emptyList());
            return null;
        }).when(springPublisher).publishEvent(any(GetAllHistoryOrdersEvent.class));

        List<HistoryOrderItem> result = adminPublisher.publishGetAllOrdersHistory(ADMIN_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
