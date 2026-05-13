package com.ticketpurchasingsystem.project.domain.HistoryOrder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.HistoryOrderService;
import com.ticketpurchasingsystem.project.application.ISystemAdminService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.application.UserService.IUserService;

@ExtendWith(MockitoExtension.class)
public class HistoryOrderServiceTests {

    @Mock
    private IHistoryOrderRepo historyOrderRepo;

    @Mock
    private HistoryOrderHandler historyOrderHandler;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private ISystemAdminService systemAdminService;

    @Mock
    private ProductionService productionService;

    @Mock
    private IUserService userService;

    private HistoryOrderService historyOrderService;

    private static final String VALID_TOKEN   = "valid-token";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final String ORDER_ID      = "order-001";
    private static final String USER_ID       = "user-001";
    private static final String EVENT_ID      = "event-001";
    private static final int    COMPANY_ID    = 42;
    private static final double PRICE         = 99.99;

    private List<String> seatIds;
    private HashMap<String, Integer> standingAreaQuantities;
    private Timestamp purchaseDate;
    private HistoryOrderItem historyOrderItem;

    @BeforeEach
    void setUp() {
        historyOrderService = new HistoryOrderService(
            historyOrderRepo, historyOrderHandler, authenticationService,
            systemAdminService, productionService, userService
        );
        seatIds = List.of("seat-1", "seat-2");
        standingAreaQuantities = new HashMap<>();
        standingAreaQuantities.put("GA", 2);
        purchaseDate = new Timestamp(System.currentTimeMillis());
        historyOrderItem = new HistoryOrderItem(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, PRICE, seatIds, standingAreaQuantities);
    }

    // --- createHistoryOrder ---

    @Test
    void WhenCreateHistoryOrderGivenValidToken_ThenReturnTrue() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(historyOrderHandler.saveHistoryOrder(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities))
            .thenReturn(historyOrderItem);

        boolean result = historyOrderService.createHistoryOrder(VALID_TOKEN, ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities);

        assertTrue(result);
    }

    @Test
    void WhenCreateHistoryOrderGivenValidToken_ThenSaveCalledOnce() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(historyOrderHandler.saveHistoryOrder(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities))
            .thenReturn(historyOrderItem);

        historyOrderService.createHistoryOrder(VALID_TOKEN, ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities);

        verify(historyOrderRepo, times(1)).save(historyOrderItem);
    }

    @Test
    void WhenCreateHistoryOrderGivenValidToken_ThenHandlerCalledWithCorrectArgs() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(historyOrderHandler.saveHistoryOrder(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities))
            .thenReturn(historyOrderItem);

        historyOrderService.createHistoryOrder(VALID_TOKEN, ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities);

        verify(historyOrderHandler, times(1)).saveHistoryOrder(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities);
    }

    @Test
    void WhenCreateHistoryOrderGivenValidToken_ThenAuthenticationCalledOnce() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(historyOrderHandler.saveHistoryOrder(any(), any(), any(), anyInt(), any(), anyDouble(), any(), any()))
            .thenReturn(historyOrderItem);

        historyOrderService.createHistoryOrder(VALID_TOKEN, ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities);

        verify(authenticationService, times(1)).validate(VALID_TOKEN);
    }

    @Test
    void WhenCreateHistoryOrderGivenInvalidToken_ThenReturnFalse() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        boolean result = historyOrderService.createHistoryOrder(INVALID_TOKEN, ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities);

        assertFalse(result);
    }

    @Test
    void WhenCreateHistoryOrderGivenInvalidToken_ThenSaveNeverCalled() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        historyOrderService.createHistoryOrder(INVALID_TOKEN, ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities);

        verify(historyOrderRepo, never()).save(any());
    }

    @Test
    void WhenCreateHistoryOrderGivenInvalidToken_ThenHandlerNeverCalled() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        historyOrderService.createHistoryOrder(INVALID_TOKEN, ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities);

        verify(historyOrderHandler, never()).saveHistoryOrder(any(), any(), any(), anyInt(), any(), anyDouble(), any(), any());
    }

    @Test
    void WhenCreateHistoryOrderGivenNullToken_ThenReturnFalse() {
        when(authenticationService.validate(null)).thenReturn(false);

        boolean result = historyOrderService.createHistoryOrder(null, ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities);

        assertFalse(result);
    }

    @Test
    void WhenCreateHistoryOrderGivenNullToken_ThenSaveNeverCalled() {
        when(authenticationService.validate(null)).thenReturn(false);

        historyOrderService.createHistoryOrder(null, ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities);

        verify(historyOrderRepo, never()).save(any());
    }

    @Test
    void WhenCreateHistoryOrderGivenHandlerReturnsNull_ThenReturnFalse() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(historyOrderHandler.saveHistoryOrder(any(), any(), any(), anyInt(), any(), anyDouble(), any(), any()))
            .thenReturn(null);

        boolean result = historyOrderService.createHistoryOrder(VALID_TOKEN, ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities);

        assertFalse(result);
    }

    @Test
    void WhenCreateHistoryOrderGivenHandlerReturnsNull_ThenSaveNeverCalled() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(historyOrderHandler.saveHistoryOrder(any(), any(), any(), anyInt(), any(), anyDouble(), any(), any()))
            .thenReturn(null);

        historyOrderService.createHistoryOrder(VALID_TOKEN, ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities);

        verify(historyOrderRepo, never()).save(any());
    }

    @Test
    void WhenCreateHistoryOrderGivenValidTokenAndEmptySeatIds_ThenReturnTrue() {
        List<String> emptySeats = List.of();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(historyOrderHandler.saveHistoryOrder(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, emptySeats, standingAreaQuantities))
            .thenReturn(new HistoryOrderItem(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, PRICE, emptySeats, standingAreaQuantities));

        boolean result = historyOrderService.createHistoryOrder(VALID_TOKEN, ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, emptySeats, standingAreaQuantities);

        assertTrue(result);
    }

    @Test
    void WhenCreateHistoryOrderGivenValidTokenAndEmptyStandingAreas_ThenReturnTrue() {
        HashMap<String, Integer> emptyStanding = new HashMap<>();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(historyOrderHandler.saveHistoryOrder(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, emptyStanding))
            .thenReturn(new HistoryOrderItem(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, PRICE, seatIds, emptyStanding));

        boolean result = historyOrderService.createHistoryOrder(VALID_TOKEN, ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, emptyStanding);

        assertTrue(result);
    }
}
