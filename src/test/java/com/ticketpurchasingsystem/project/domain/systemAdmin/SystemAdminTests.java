package com.ticketpurchasingsystem.project.domain.systemAdmin;

import java.util.Collections;
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
        AdminInfo adminInfo = new AdminInfo("testAdmin", "test@test.com");
        systemAdmin = new SystemAdmin(adminInfo, adminPublisher);
    }

    // --- getAllActiveOrders ---

    @Test
    void WhenGetAllActiveOrdersGivenPublisherReturnsOrders_ThenReturnOrders() {
        List<ActiveOrderItem> mockOrders = List.of(new ActiveOrderItem("1", "1", "1", 1));
        when(adminPublisher.publishGetAllActiveOrders(anyString())).thenReturn(mockOrders);

        List<ActiveOrderItem> result = systemAdmin.getAllActiveOrders();

        assertEquals(mockOrders, result);
    }

    @Test
    void WhenGetAllActiveOrdersGivenPublisherReturnsNull_ThenReturnNull() {
        when(adminPublisher.publishGetAllActiveOrders(anyString())).thenReturn(null);

        assertNull(systemAdmin.getAllActiveOrders());
    }

    @Test
    void WhenGetAllActiveOrdersGivenPublisherReturnsEmpty_ThenReturnEmpty() {
        when(adminPublisher.publishGetAllActiveOrders(anyString())).thenReturn(Collections.emptyList());

        assertTrue(systemAdmin.getAllActiveOrders().isEmpty());
    }

    @Test
    void WhenGetAllActiveOrdersGiven_ThenPublisherCalledWithAdminId() {
        when(adminPublisher.publishGetAllActiveOrders(anyString())).thenReturn(Collections.emptyList());

        systemAdmin.getAllActiveOrders();

        verify(adminPublisher, times(1)).publishGetAllActiveOrders(anyString());
    }

    // --- getAllHistoryOrderItems ---

    @Test
    void WhenGetAllHistoryOrderItemsGivenPublisherReturnsHistory_ThenReturnHistory() {
        List<HistoryOrderItem> mockHistory = List.of(new HistoryOrderItem());
        when(adminPublisher.publishGetAllOrdersHistory(anyString())).thenReturn(mockHistory);

        List<HistoryOrderItem> result = systemAdmin.getAllHistoryOrderItems();

        assertEquals(mockHistory, result);
    }

    @Test
    void WhenGetAllHistoryOrderItemsGivenPublisherReturnsNull_ThenReturnNull() {
        when(adminPublisher.publishGetAllOrdersHistory(anyString())).thenReturn(null);

        assertNull(systemAdmin.getAllHistoryOrderItems());
    }

    @Test
    void WhenGetAllHistoryOrderItemsGivenPublisherReturnsEmpty_ThenReturnEmpty() {
        when(adminPublisher.publishGetAllOrdersHistory(anyString())).thenReturn(Collections.emptyList());

        assertTrue(systemAdmin.getAllHistoryOrderItems().isEmpty());
    }

    @Test
    void WhenGetAllHistoryOrderItemsGiven_ThenPublisherCalledWithAdminId() {
        when(adminPublisher.publishGetAllOrdersHistory(anyString())).thenReturn(Collections.emptyList());

        systemAdmin.getAllHistoryOrderItems();

        verify(adminPublisher, times(1)).publishGetAllOrdersHistory(anyString());
    }
}
