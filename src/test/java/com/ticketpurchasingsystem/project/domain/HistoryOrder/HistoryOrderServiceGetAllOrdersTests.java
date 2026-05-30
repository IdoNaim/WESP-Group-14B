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
public class HistoryOrderServiceGetAllOrdersTests {

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
    void GivenInvalidSession_WhenGetAllHistoryOrders_ThenThrowRuntimeException() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> historyOrderService.getAllHistoryOrders(INVALID_SESSION));
        verify(historyOrderRepo, never()).findAll();
    }

    @Test
    void GivenInvalidSession_WhenGetAllHistoryOrders_ThenRepoNeverCalled() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> historyOrderService.getAllHistoryOrders(INVALID_SESSION));
        verify(historyOrderRepo, never()).findAll();
    }

    // --- valid session, not admin ---

    @Test
    void GivenValidSessionNotAdmin_WhenGetAllHistoryOrders_ThenThrowSecurityException() {
        stubNonAdminSession();

        assertThrows(SecurityException.class,
                () -> historyOrderService.getAllHistoryOrders(VALID_SESSION));
        verify(historyOrderRepo, never()).findAll();
    }

    @Test
    void GivenValidSessionNotAdmin_WhenGetAllHistoryOrders_ThenRepoNeverCalled() {
        stubNonAdminSession();

        assertThrows(SecurityException.class,
                () -> historyOrderService.getAllHistoryOrders(VALID_SESSION));
        verify(historyOrderRepo, never()).findAll();
    }

    // --- admin ---

    @Test
    void GivenAdminNoOrders_WhenGetAllHistoryOrders_ThenReturnEmptyList() {
        stubAdminSession();
        when(historyOrderRepo.findAll()).thenReturn(List.of());

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrders(VALID_SESSION);

        assertTrue(result.isEmpty());
        verify(historyOrderRepo, times(1)).findAll();
    }

    @Test
    void GivenAdminWithOrders_WhenGetAllHistoryOrders_ThenReturnAllDTOs() {
        stubAdminSession();
        HistoryOrderItem item2 = new HistoryOrderItem("order-002", "user-002", "event-002", COMPANY_ID, 49.99,
            List.of("seat-2"), new HashMap<>());
        when(historyOrderRepo.findAll()).thenReturn(List.of(item, item2));

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrders(VALID_SESSION);

        assertEquals(2, result.size());
        assertEquals(ORDER_ID, result.get(0).getOrderId());
        verify(historyOrderRepo, times(1)).findAll();
    }

    @Test
    void GivenAdminWithOrders_WhenGetAllHistoryOrders_ThenReturnCorrectDTOs() {
        stubAdminSession();
        when(historyOrderRepo.findAll()).thenReturn(List.of(item));

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrders(VALID_SESSION);

        assertEquals(ORDER_ID, result.get(0).getOrderId());
        assertEquals(USER_ID, result.get(0).getUserId());
        assertEquals(EVENT_ID, result.get(0).getEventId());
        verify(historyOrderRepo, times(1)).findAll();
    }

    @Test
    void GivenAdmin_WhenGetAllHistoryOrders_ThenRepoCalledOnce() {
        stubAdminSession();
        when(historyOrderRepo.findAll()).thenReturn(List.of());

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrders(VALID_SESSION);

        assertTrue(result.isEmpty());
        verify(historyOrderRepo, times(1)).findAll();
    }
}
