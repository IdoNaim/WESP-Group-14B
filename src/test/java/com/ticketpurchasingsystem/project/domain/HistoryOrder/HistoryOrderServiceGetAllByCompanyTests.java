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
public class HistoryOrderServiceGetAllByCompanyTests {

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

    private void stubValidSession() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
    }

    // --- invalid session ---

    @Test
    void GivenInvalidSession_WhenGetAllHistoryOrdersByCompany_ThenReturnEmptyList() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        assertTrue(historyOrderService.getAllHistoryOrdersByCompany(INVALID_SESSION, COMPANY_ID).isEmpty());
    }

    @Test
    void GivenInvalidSession_WhenGetAllHistoryOrdersByCompany_ThenRepoNeverCalled() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        historyOrderService.getAllHistoryOrdersByCompany(INVALID_SESSION, COMPANY_ID);

        verify(historyOrderRepo, never()).findAllByCompanyId(anyInt());
    }

    // --- valid session, no orders ---

    @Test
    void GivenValidSessionAndNoOrders_WhenGetAllHistoryOrdersByCompany_ThenReturnEmptyList() {
        stubValidSession();
        when(historyOrderRepo.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of());

        assertTrue(historyOrderService.getAllHistoryOrdersByCompany(VALID_SESSION, COMPANY_ID).isEmpty());
    }

    @Test
    void GivenValidSession_WhenGetAllHistoryOrdersByCompany_ThenRepoCalledOnceWithCorrectCompanyId() {
        stubValidSession();
        when(historyOrderRepo.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of());

        historyOrderService.getAllHistoryOrdersByCompany(VALID_SESSION, COMPANY_ID);

        verify(historyOrderRepo, times(1)).findAllByCompanyId(COMPANY_ID);
    }

    // --- valid session, orders found ---

    @Test
    void GivenValidSessionAndOneOrder_WhenGetAllHistoryOrdersByCompany_ThenReturnOneDTO() {
        stubValidSession();
        when(historyOrderRepo.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of(item));

        assertEquals(1, historyOrderService.getAllHistoryOrdersByCompany(VALID_SESSION, COMPANY_ID).size());
    }

    @Test
    void GivenValidSessionAndMultipleOrders_WhenGetAllHistoryOrdersByCompany_ThenReturnAllDTOs() {
        stubValidSession();
        HistoryOrderItem item2 = new HistoryOrderItem("order-002", "user-002", "event-002", COMPANY_ID, 49.99,
            List.of("seat-2"), new HashMap<>());
        when(historyOrderRepo.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of(item, item2));

        assertEquals(2, historyOrderService.getAllHistoryOrdersByCompany(VALID_SESSION, COMPANY_ID).size());
    }

    @Test
    void GivenValidSessionAndOrderExists_WhenGetAllHistoryOrdersByCompany_ThenReturnCorrectDTO() {
        stubValidSession();
        when(historyOrderRepo.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of(item));

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrdersByCompany(VALID_SESSION, COMPANY_ID);

        assertEquals(ORDER_ID, result.get(0).getOrderId());
        assertEquals(USER_ID, result.get(0).getUserId());
        assertEquals(EVENT_ID, result.get(0).getEventId());
    }
}
