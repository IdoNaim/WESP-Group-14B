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
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;

@ExtendWith(MockitoExtension.class)
public class HistoryOrderServiceTests {

    @Mock private IHistoryOrderRepo historyOrderRepo;
    @Mock private HistoryOrderHandler historyOrderHandler;
    @Mock private AuthenticationService authenticationService;
    @Mock private ProductionService productionService;

    private HistoryOrderService historyOrderService;

    private static final String ORDER_ID   = "order-001";
    private static final String USER_ID    = "user-001";
    private static final String EVENT_ID   = "event-001";
    private static final int    COMPANY_ID = 42;
    private static final double PRICE      = 99.99;

    private List<String> seatIds;
    private HashMap<String, Integer> standingAreaQuantities;
    private Timestamp purchaseDate;
    private HistoryOrderItem historyOrderItem;

    @BeforeEach
    void setUp() {
        historyOrderService = new HistoryOrderService(
            historyOrderRepo, historyOrderHandler, authenticationService, productionService
        );
        seatIds = List.of("seat-1", "seat-2");
        standingAreaQuantities = new HashMap<>();
        standingAreaQuantities.put("GA", 2);
        purchaseDate = new Timestamp(System.currentTimeMillis());
        historyOrderItem = new HistoryOrderItem(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, PRICE, seatIds, standingAreaQuantities);
    }

    @Test
    void GivenValidArgs_WhenCreateHistoryOrder_ThenReturnTrue() {
        when(historyOrderHandler.saveHistoryOrder(any(HistoryOrderDTO.class))).thenReturn(historyOrderItem);

        boolean result = historyOrderService.createHistoryOrder(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities);

        assertTrue(result);
    }

    @Test
    void GivenValidArgs_WhenCreateHistoryOrder_ThenSaveCalledOnce() {
        when(historyOrderHandler.saveHistoryOrder(any(HistoryOrderDTO.class))).thenReturn(historyOrderItem);

        historyOrderService.createHistoryOrder(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities);

        verify(historyOrderRepo, times(1)).save(historyOrderItem);
    }

    @Test
    void GivenValidArgs_WhenCreateHistoryOrder_ThenHandlerCalledOnce() {
        when(historyOrderHandler.saveHistoryOrder(any(HistoryOrderDTO.class))).thenReturn(historyOrderItem);

        historyOrderService.createHistoryOrder(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities);

        verify(historyOrderHandler, times(1)).saveHistoryOrder(any(HistoryOrderDTO.class));
    }

    @Test
    void GivenValidArgs_WhenCreateHistoryOrder_ThenHandlerCalledWithCorrectDTO() {
        when(historyOrderHandler.saveHistoryOrder(any(HistoryOrderDTO.class))).thenReturn(historyOrderItem);

        historyOrderService.createHistoryOrder(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities);

        verify(historyOrderHandler).saveHistoryOrder(argThat(dto ->
            ORDER_ID.equals(dto.getOrderId()) &&
            USER_ID.equals(dto.getUserId()) &&
            EVENT_ID.equals(dto.getEventId()) &&
            dto.getCompanyId() == COMPANY_ID &&
            dto.getPrice() == PRICE
        ));
    }

    @Test
    void GivenHandlerReturnsNull_WhenCreateHistoryOrder_ThenReturnFalse() {
        when(historyOrderHandler.saveHistoryOrder(any(HistoryOrderDTO.class))).thenReturn(null);

        boolean result = historyOrderService.createHistoryOrder(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities);

        assertFalse(result);
    }

    @Test
    void GivenHandlerReturnsNull_WhenCreateHistoryOrder_ThenSaveNeverCalled() {
        when(historyOrderHandler.saveHistoryOrder(any(HistoryOrderDTO.class))).thenReturn(null);

        historyOrderService.createHistoryOrder(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, standingAreaQuantities);

        verify(historyOrderRepo, never()).save(any());
    }

    @Test
    void GivenEmptySeatIds_WhenCreateHistoryOrder_ThenReturnTrue() {
        List<String> emptySeats = List.of();
        HistoryOrderItem item = new HistoryOrderItem(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, PRICE, emptySeats, standingAreaQuantities);
        when(historyOrderHandler.saveHistoryOrder(any(HistoryOrderDTO.class))).thenReturn(item);

        boolean result = historyOrderService.createHistoryOrder(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, emptySeats, standingAreaQuantities);

        assertTrue(result);
    }

    @Test
    void GivenEmptyStandingAreas_WhenCreateHistoryOrder_ThenReturnTrue() {
        HashMap<String, Integer> emptyStanding = new HashMap<>();
        HistoryOrderItem item = new HistoryOrderItem(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, PRICE, seatIds, emptyStanding);
        when(historyOrderHandler.saveHistoryOrder(any(HistoryOrderDTO.class))).thenReturn(item);

        boolean result = historyOrderService.createHistoryOrder(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, purchaseDate, PRICE, seatIds, emptyStanding);

        assertTrue(result);
    }
}
