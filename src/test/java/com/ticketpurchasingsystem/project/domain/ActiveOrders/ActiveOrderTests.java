package com.ticketpurchasingsystem.project.domain.ActiveOrders;
import com.ticketpurchasingsystem.project.application.IActiveOrderService;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
import com.ticketpurchasingsystem.project.infrastructure.ActiveOrderMemRepo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import com.ticketpurchasingsystem.project.application.ActiveOrderService;

public class ActiveOrderTests {
    private IActiveOrderRepo activeOrderRepoMock;
    private IActiveOrderService activeOrderService;
    //private ActiveOrderListener activeOrderListener;
    private ActiveOrderPublisher activeOrderPublisher;
    @BeforeEach
    public void setUp() {
        activeOrderRepoMock = mock(IActiveOrderRepo.class);
        activeOrderPublisher = mock(ActiveOrderPublisher.class);
        activeOrderService = new ActiveOrderService(new ActiveOrderListener(activeOrderRepoMock), activeOrderPublisher, activeOrderRepoMock);

    }
    @Test
    public void GivenValidOrder_WhenSaveOrder_thenReturnTrue() {
        ActiveOrderItem activeOrder = new ActiveOrderItem("1", "1","1",1);
        when(activeOrderRepoMock.save(activeOrder)).thenReturn(true);
        when(activeOrderPublisher.publishIsValidEventIDEvent(activeOrder.getEventId())).thenReturn(true);
        boolean result = activeOrderService.saveOrder(activeOrder);
        assertTrue(result);
    }
    @Test
    public void GivenValidOrder_WhenSaveOrder_thenCallSaveInRepo() {
        ActiveOrderItem activeOrder = new ActiveOrderItem("1", "1","1",1);
        when(activeOrderRepoMock.save(activeOrder)).thenReturn(true);
        when(activeOrderPublisher.publishIsValidEventIDEvent(activeOrder.getEventId())).thenReturn(true);
        boolean result = activeOrderService.saveOrder(activeOrder);
        verify(activeOrderRepoMock, times(1)).save(activeOrder);
        assertTrue(result);
    }
    @Test
    public void GivenThrownException_WhenSaveOrder_thenReturnFalse() {
        ActiveOrderItem activeOrder = new ActiveOrderItem("1", "1","1",1);
        when(activeOrderRepoMock.save(activeOrder)).thenThrow(new RuntimeException(" got error when saving order"));
        when(activeOrderPublisher.publishIsValidEventIDEvent(activeOrder.getEventId())).thenReturn(true);
        boolean result = activeOrderService.saveOrder(activeOrder);
        assertFalse(result);
    }
    @Test
    public void GivenNullOrder_WhenSaveOrder_thenReturnFalse() {
        boolean result = activeOrderService.saveOrder(null);
        assertFalse(result);
    }


    @Test
    public void GivenInvalidEventId_WhenSaveOrder_thenReturnErrorMessage() {
        ActiveOrderItem activeOrder = new ActiveOrderItem("1", "1","1",1);
        when(activeOrderPublisher.publishIsValidEventIDEvent(activeOrder.getEventId())).thenReturn(false);
        boolean result = activeOrderService.saveOrder(activeOrder);
        assertFalse(result);
    }

    @Test
    public void GivenInvalidQuantity_WhenSaveOrder_thenReturnErrorMessage() {
        ActiveOrderItem activeOrder = new ActiveOrderItem("1", "1","1",1);
        when(activeOrderPublisher.publishIsValidEventIDEvent(activeOrder.getEventId())).thenReturn(true);
        boolean result = activeOrderService.saveOrder(activeOrder);
        assertFalse(result);
    }

    @Test
    public void GivenInvalidOrderId_WhenSaveOrder_thenReturnErrorMessage() {
        ActiveOrderItem activeOrder = new ActiveOrderItem("1", "1","1",1);
        when(activeOrderPublisher.publishIsValidEventIDEvent(activeOrder.getEventId())).thenReturn(true);
        boolean result = activeOrderService.saveOrder(activeOrder);
        assertFalse(result);
    }
    @Test
    public void GivenValidOrderId_WhenGetOrderInfo_thenReturnOrderInfo() {
        String orderId = "1";
        String userId = "0";
        String eventId = "0";
        int quantity = 2;
        
        ActiveOrderItem activeOrder = new ActiveOrderItem(orderId, userId, eventId, quantity);
        when(activeOrderRepoMock.findById(orderId)).thenReturn(activeOrder);
        ActiveOrderDTO result = activeOrderService.getOrderInfo(orderId);
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(userId, result.getUserId());
        assertEquals(eventId, result.getEventId());
        assertEquals(quantity, result.getQuantity());
    }
    @Test
    public void GivenInvalidOrderId_WhenGetOrderInfo_thenReturnNull() {
        String orderId = "1";
        when(activeOrderRepoMock.findById(orderId)).thenReturn(null);
        ActiveOrderDTO result = activeOrderService.getOrderInfo(orderId);
        assertNull(result);
    }

}
