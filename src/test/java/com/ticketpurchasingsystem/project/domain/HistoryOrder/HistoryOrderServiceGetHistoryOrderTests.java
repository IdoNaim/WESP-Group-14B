package com.ticketpurchasingsystem.project.domain.HistoryOrder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.HistoryOrderService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

@ExtendWith(MockitoExtension.class)
public class HistoryOrderServiceGetHistoryOrderTests {

    @Mock private IHistoryOrderRepo historyOrderRepo;
    @Mock private HistoryOrderHandler historyOrderHandler;
    @Mock private AuthenticationService authenticationService;
    @Mock private ProductionService productionService;

    private HistoryOrderService historyOrderService;

    private static final String VALID_TOKEN    = "valid-token";
    private static final String INVALID_TOKEN  = "invalid-token";
    private static final String ORDER_ID       = "order-001";
    private static final String USER_ID        = "user-001";
    private static final String OTHER_USER_ID  = "user-002";
    private static final String EVENT_ID       = "event-001";
    private static final int    COMPANY_ID     = 42;
    private static final double PRICE          = 99.99;

    private static final SessionToken VALID_SESSION   = new SessionToken(VALID_TOKEN, 9999999999L);
    private static final SessionToken INVALID_SESSION = new SessionToken(INVALID_TOKEN, 9999999999L);

    private HistoryOrderItem item;

    @BeforeEach
    void setUp() {
        historyOrderService = new HistoryOrderService(
            historyOrderRepo, historyOrderHandler, authenticationService, productionService
        );
        item = new HistoryOrderItem(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, PRICE,
            List.of("seat-1"), new HashMap<>());
    }

    private void stubAdminSession() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.isAdmin(VALID_TOKEN)).thenReturn(true);
    }

    private void stubOwnerSession() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.isAdmin(VALID_TOKEN)).thenReturn(false);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
    }

    private void stubNonOwnerSession() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.isAdmin(VALID_TOKEN)).thenReturn(false);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(OTHER_USER_ID);
    }

    // --- invalid session ---

    @Test
    void GivenInvalidSession_WhenGetHistoryOrder_ThenReturnNull() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        HistoryOrderDTO result = historyOrderService.getHistoryOrder(INVALID_SESSION, ORDER_ID);

        assertNull(result);
        verify(historyOrderRepo, never()).findByOrderId(any());
    }

    @Test
    void GivenInvalidSession_WhenGetHistoryOrder_ThenRepoNeverCalled() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        HistoryOrderDTO result = historyOrderService.getHistoryOrder(INVALID_SESSION, ORDER_ID);

        assertNull(result);
        verify(historyOrderRepo, never()).findByOrderId(any());
    }

    // --- valid session, order not found ---

    @Test
    void GivenValidSessionAndOrderNotFound_WhenGetHistoryOrder_ThenReturnNull() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(historyOrderRepo.findByOrderId(ORDER_ID)).thenReturn(null);

        HistoryOrderDTO result = historyOrderService.getHistoryOrder(VALID_SESSION, ORDER_ID);

        assertNull(result);
        verify(historyOrderRepo, times(1)).findByOrderId(ORDER_ID);
    }

    @Test
    void GivenValidSessionAndOrderNotFound_WhenGetHistoryOrder_ThenRepoCalledOnce() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(historyOrderRepo.findByOrderId(ORDER_ID)).thenReturn(null);

        HistoryOrderDTO result = historyOrderService.getHistoryOrder(VALID_SESSION, ORDER_ID);

        assertNull(result);
        verify(historyOrderRepo, times(1)).findByOrderId(ORDER_ID);
    }

    // --- admin, order found ---

    @Test
    void GivenAdminAndOrderFound_WhenGetHistoryOrder_ThenReturnDTO() {
        stubAdminSession();
        when(historyOrderRepo.findByOrderId(ORDER_ID)).thenReturn(item);

        HistoryOrderDTO result = historyOrderService.getHistoryOrder(VALID_SESSION, ORDER_ID);

        assertNotNull(result);
        verify(historyOrderRepo, times(1)).findByOrderId(ORDER_ID);
    }

    @Test
    void GivenAdminAndOrderFound_WhenGetHistoryOrder_ThenReturnCorrectDTO() {
        stubAdminSession();
        when(historyOrderRepo.findByOrderId(ORDER_ID)).thenReturn(item);

        HistoryOrderDTO result = historyOrderService.getHistoryOrder(VALID_SESSION, ORDER_ID);

        assertEquals(ORDER_ID, result.getOrderId());
        assertEquals(USER_ID, result.getUserId());
        assertEquals(EVENT_ID, result.getEventId());
        verify(historyOrderRepo, times(1)).findByOrderId(ORDER_ID);
    }

    @Test
    void GivenAdminAndOrderFound_WhenGetHistoryOrder_ThenRepoCalledWithCorrectOrderId() {
        stubAdminSession();
        when(historyOrderRepo.findByOrderId(ORDER_ID)).thenReturn(item);

        HistoryOrderDTO result = historyOrderService.getHistoryOrder(VALID_SESSION, ORDER_ID);

        assertNotNull(result);
        verify(historyOrderRepo, times(1)).findByOrderId(ORDER_ID);
    }

    // --- owner (non-admin), order found ---

    @Test
    void GivenOwnerAndOrderFound_WhenGetHistoryOrder_ThenReturnDTO() {
        stubOwnerSession();
        when(historyOrderRepo.findByOrderId(ORDER_ID)).thenReturn(item);

        HistoryOrderDTO result = historyOrderService.getHistoryOrder(VALID_SESSION, ORDER_ID);

        assertNotNull(result);
        verify(historyOrderRepo, times(1)).findByOrderId(ORDER_ID);
    }

    @Test
    void GivenOwnerAndOrderFound_WhenGetHistoryOrder_ThenReturnCorrectDTO() {
        stubOwnerSession();
        when(historyOrderRepo.findByOrderId(ORDER_ID)).thenReturn(item);

        HistoryOrderDTO result = historyOrderService.getHistoryOrder(VALID_SESSION, ORDER_ID);

        assertEquals(ORDER_ID, result.getOrderId());
        assertEquals(USER_ID, result.getUserId());
        assertEquals(EVENT_ID, result.getEventId());
        verify(historyOrderRepo, times(1)).findByOrderId(ORDER_ID);
    }

    // --- non-owner, non-admin, order found ---

    @Test
    void GivenNonOwnerNonAdminAndOrderFound_WhenGetHistoryOrder_ThenReturnNull() {
        stubNonOwnerSession();
        when(historyOrderRepo.findByOrderId(ORDER_ID)).thenReturn(item);

        HistoryOrderDTO result = historyOrderService.getHistoryOrder(VALID_SESSION, ORDER_ID);

        assertNull(result);
        verify(historyOrderRepo, times(1)).findByOrderId(ORDER_ID);
    }
}
