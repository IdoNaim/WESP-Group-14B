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

    private static final String VALID_TOKEN   = "valid-token";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final String ORDER_ID      = "order-001";
    private static final String USER_ID       = "user-001";
    private static final String EVENT_ID      = "event-001";
    private static final int    COMPANY_ID    = 42;
    private static final double PRICE         = 99.99;

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

    private void stubNonAdminSession() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.isAdmin(VALID_TOKEN)).thenReturn(false);
    }

    // --- invalid session ---

    @Test
    void GivenInvalidSession_WhenGetHistoryOrder_ThenReturnNull() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        assertNull(historyOrderService.getHistoryOrder(INVALID_SESSION, ORDER_ID));
    }

    @Test
    void GivenInvalidSession_WhenGetHistoryOrder_ThenRepoNeverCalled() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        historyOrderService.getHistoryOrder(INVALID_SESSION, ORDER_ID);

        verify(historyOrderRepo, never()).findByOrderId(any());
    }

    // --- valid session, not admin ---

    @Test
    void GivenValidSessionNotAdmin_WhenGetHistoryOrder_ThenReturnNull() {
        stubNonAdminSession();

        assertNull(historyOrderService.getHistoryOrder(VALID_SESSION, ORDER_ID));
    }

    @Test
    void GivenValidSessionNotAdmin_WhenGetHistoryOrder_ThenRepoNeverCalled() {
        stubNonAdminSession();

        historyOrderService.getHistoryOrder(VALID_SESSION, ORDER_ID);

        verify(historyOrderRepo, never()).findByOrderId(any());
    }

    // --- admin, order not found ---

    @Test
    void GivenAdminAndOrderNotFound_WhenGetHistoryOrder_ThenReturnNull() {
        stubAdminSession();
        when(historyOrderRepo.findByOrderId(ORDER_ID)).thenReturn(null);

        assertNull(historyOrderService.getHistoryOrder(VALID_SESSION, ORDER_ID));
    }

    // --- admin, order found ---

    @Test
    void GivenAdminAndOrderExists_WhenGetHistoryOrder_ThenReturnDTO() {
        stubAdminSession();
        when(historyOrderRepo.findByOrderId(ORDER_ID)).thenReturn(item);

        assertNotNull(historyOrderService.getHistoryOrder(VALID_SESSION, ORDER_ID));
    }

    @Test
    void GivenAdminAndOrderExists_WhenGetHistoryOrder_ThenReturnCorrectDTO() {
        stubAdminSession();
        when(historyOrderRepo.findByOrderId(ORDER_ID)).thenReturn(item);

        HistoryOrderDTO result = historyOrderService.getHistoryOrder(VALID_SESSION, ORDER_ID);

        assertEquals(ORDER_ID, result.getOrderId());
        assertEquals(USER_ID, result.getUserId());
        assertEquals(EVENT_ID, result.getEventId());
    }

    @Test
    void GivenAdmin_WhenGetHistoryOrder_ThenRepoCalledWithCorrectOrderId() {
        stubAdminSession();
        when(historyOrderRepo.findByOrderId(ORDER_ID)).thenReturn(item);

        historyOrderService.getHistoryOrder(VALID_SESSION, ORDER_ID);

        verify(historyOrderRepo, times(1)).findByOrderId(ORDER_ID);
    }
}
