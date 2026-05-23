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
public class HistoryOrderServiceGetAllByUserTests {

    @Mock private IHistoryOrderRepo historyOrderRepo;
    @Mock private HistoryOrderHandler historyOrderHandler;
    @Mock private AuthenticationService authenticationService;
    @Mock private ProductionService productionService;

    private HistoryOrderService historyOrderService;

    private static final String VALID_TOKEN   = "valid-token";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final String USER_ID       = "user-001";
    private static final String OTHER_USER_ID = "user-002";
    private static final String EVENT_ID      = "event-001";
    private static final String ORDER_ID      = "order-001";
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

    private void stubValidSession(String tokenOwner, boolean isAdmin) {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(tokenOwner);
        when(authenticationService.isAdmin(VALID_TOKEN)).thenReturn(isAdmin);
    }

    // --- invalid session ---

    @Test
    void GivenInvalidSession_WhenGetAllHistoryOrdersByUser_ThenReturnEmptyList() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrdersByUser(INVALID_SESSION, USER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void GivenInvalidSession_WhenGetAllHistoryOrdersByUser_ThenRepoNeverCalled() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        historyOrderService.getAllHistoryOrdersByUser(INVALID_SESSION, USER_ID);

        verify(historyOrderRepo, never()).findAllByUserId(any());
    }

    // --- valid session, not owner, not admin ---

    @Test
    void GivenOtherUserNotAdmin_WhenGetAllHistoryOrdersByUser_ThenReturnEmptyList() {
        stubValidSession(OTHER_USER_ID, false);

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrdersByUser(VALID_SESSION, USER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void GivenOtherUserNotAdmin_WhenGetAllHistoryOrdersByUser_ThenRepoNeverCalled() {
        stubValidSession(OTHER_USER_ID, false);

        historyOrderService.getAllHistoryOrdersByUser(VALID_SESSION, USER_ID);

        verify(historyOrderRepo, never()).findAllByUserId(any());
    }

    // --- owner (not admin) ---

    @Test
    void GivenOwner_WhenGetAllHistoryOrdersByUser_ThenReturnOrders() {
        stubValidSession(USER_ID, false);
        when(historyOrderRepo.findAllByUserId(USER_ID)).thenReturn(List.of(item));

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrdersByUser(VALID_SESSION, USER_ID);

        assertEquals(1, result.size());
    }

    @Test
    void GivenOwnerNoOrders_WhenGetAllHistoryOrdersByUser_ThenReturnEmptyList() {
        stubValidSession(USER_ID, false);
        when(historyOrderRepo.findAllByUserId(USER_ID)).thenReturn(List.of());

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrdersByUser(VALID_SESSION, USER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void GivenOwnerMultipleOrders_WhenGetAllHistoryOrdersByUser_ThenReturnAllDTOs() {
        stubValidSession(USER_ID, false);
        HistoryOrderItem item2 = new HistoryOrderItem("order-002", USER_ID, "event-002", COMPANY_ID, 49.99,
            List.of("seat-2"), new HashMap<>());
        when(historyOrderRepo.findAllByUserId(USER_ID)).thenReturn(List.of(item, item2));

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrdersByUser(VALID_SESSION, USER_ID);

        assertEquals(2, result.size());
    }

    // --- admin (not owner) ---

    @Test
    void GivenAdmin_WhenGetAllHistoryOrdersByUser_ThenReturnOrders() {
        stubValidSession(OTHER_USER_ID, true);
        when(historyOrderRepo.findAllByUserId(USER_ID)).thenReturn(List.of(item));

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrdersByUser(VALID_SESSION, USER_ID);

        assertEquals(1, result.size());
    }

    @Test
    void GivenAdmin_WhenGetAllHistoryOrdersByUser_ThenReturnCorrectDTOs() {
        stubValidSession(OTHER_USER_ID, true);
        when(historyOrderRepo.findAllByUserId(USER_ID)).thenReturn(List.of(item));

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrdersByUser(VALID_SESSION, USER_ID);

        assertEquals(ORDER_ID, result.get(0).getOrderId());
        assertEquals(USER_ID, result.get(0).getUserId());
        assertEquals(EVENT_ID, result.get(0).getEventId());
    }

    @Test
    void GivenAdmin_WhenGetAllHistoryOrdersByUser_ThenRepoCalledWithCorrectUserId() {
        stubValidSession(OTHER_USER_ID, true);
        when(historyOrderRepo.findAllByUserId(USER_ID)).thenReturn(List.of());

        historyOrderService.getAllHistoryOrdersByUser(VALID_SESSION, USER_ID);

        verify(historyOrderRepo, times(1)).findAllByUserId(USER_ID);
    }
}
