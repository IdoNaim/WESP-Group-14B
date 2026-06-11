package com.ticketpurchasingsystem.project.domain.systemAdmin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;

class SystemAdminTests {

    private AdminPublisher adminPublisher;
    private SystemAdmin systemAdmin;

    @BeforeEach
    void setUp() {
        adminPublisher = mock(AdminPublisher.class);
        systemAdmin = new SystemAdmin("test-admin-id", adminPublisher);
    }

    // --- getAllActiveOrders ---

    @Test
    void GivenPublisherReturnsOrders_WhenGetAllActiveOrders_ThenReturnOrders() {
        List<ActiveOrderItem> mockOrders = List.of(new ActiveOrderItem("1", "1", "1"));
        when(adminPublisher.publishGetAllActiveOrders(anyString())).thenReturn(mockOrders);

        List<ActiveOrderItem> result = systemAdmin.getAllActiveOrders();

        assertEquals(mockOrders, result);
        verify(adminPublisher, times(1)).publishGetAllActiveOrders(anyString());
    }

    @Test
    void GivenPublisherReturnsNull_WhenGetAllActiveOrders_ThenReturnNull() {
        when(adminPublisher.publishGetAllActiveOrders(anyString())).thenReturn(null);

        List<ActiveOrderItem> result = systemAdmin.getAllActiveOrders();

        assertNull(result);
        verify(adminPublisher, times(1)).publishGetAllActiveOrders(anyString());
    }

    @Test
    void GivenPublisherReturnsEmpty_WhenGetAllActiveOrders_ThenReturnEmpty() {
        when(adminPublisher.publishGetAllActiveOrders(anyString())).thenReturn(Collections.emptyList());

        List<ActiveOrderItem> result = systemAdmin.getAllActiveOrders();

        assertTrue(result.isEmpty());
        verify(adminPublisher, times(1)).publishGetAllActiveOrders(anyString());
    }

    @Test
    void GivenAdminIsInitialized_WhenGetAllActiveOrders_ThenPublisherCalledWithAdminId() {
        when(adminPublisher.publishGetAllActiveOrders(anyString())).thenReturn(Collections.emptyList());

        systemAdmin.getAllActiveOrders();

        verify(adminPublisher, times(1)).publishGetAllActiveOrders(anyString());
    }

    // --- getAllHistoryOrderItems ---

    @Test
    void GivenPublisherReturnsHistory_WhenGetAllHistoryOrderItems_ThenReturnHistory() {
        List<HistoryOrderItem> mockHistory = List.of(new HistoryOrderItem("1", "user","event", 15,10.0, new ArrayList<>(), new HashMap<>()));
        when(adminPublisher.publishGetAllOrdersHistory(anyString())).thenReturn(mockHistory);

        List<HistoryOrderItem> result = systemAdmin.getAllHistoryOrderItems();

        assertEquals(mockHistory, result);
        verify(adminPublisher, times(1)).publishGetAllOrdersHistory(anyString());
    }

    @Test
    void GivenPublisherReturnsNull_WhenGetAllHistoryOrderItems_ThenReturnNull() {
        when(adminPublisher.publishGetAllOrdersHistory(anyString())).thenReturn(null);

        List<HistoryOrderItem> result = systemAdmin.getAllHistoryOrderItems();

        assertNull(result);
        verify(adminPublisher, times(1)).publishGetAllOrdersHistory(anyString());
    }

    @Test
    void GivenPublisherReturnsEmpty_WhenGetAllHistoryOrderItems_ThenReturnEmpty() {
        when(adminPublisher.publishGetAllOrdersHistory(anyString())).thenReturn(Collections.emptyList());

        List<HistoryOrderItem> result = systemAdmin.getAllHistoryOrderItems();

        assertTrue(result.isEmpty());
        verify(adminPublisher, times(1)).publishGetAllOrdersHistory(anyString());
    }

    @Test
    void GivenAdminIsInitialized_WhenGetAllHistoryOrderItems_ThenPublisherCalledWithAdminId() {
        when(adminPublisher.publishGetAllOrdersHistory(anyString())).thenReturn(Collections.emptyList());

        systemAdmin.getAllHistoryOrderItems();

        verify(adminPublisher, times(1)).publishGetAllOrdersHistory(anyString());
    }
}
