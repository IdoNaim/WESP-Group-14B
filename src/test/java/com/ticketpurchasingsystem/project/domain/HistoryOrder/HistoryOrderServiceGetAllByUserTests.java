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
import com.ticketpurchasingsystem.project.application.ISystemAdminService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.application.UserService.IUserService;
import com.ticketpurchasingsystem.project.domain.User.UserDTO;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

@ExtendWith(MockitoExtension.class)
public class HistoryOrderServiceGetAllByUserTests {

    @Mock private IHistoryOrderRepo historyOrderRepo;
    @Mock private HistoryOrderHandler historyOrderHandler;
    @Mock private AuthenticationService authenticationService;
    @Mock private ISystemAdminService systemAdminService;
    @Mock private ProductionService productionService;
    @Mock private IUserService userService;

    private HistoryOrderService historyOrderService;

    private static final String VALID_TOKEN   = "valid-token";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final String USER_ID       = "user-001";
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
            historyOrderRepo, historyOrderHandler, authenticationService,
            systemAdminService, productionService, userService
        );
        item = new HistoryOrderItem(ORDER_ID, USER_ID, EVENT_ID, COMPANY_ID, PRICE,
            List.of("seat-1"), new HashMap<>());
    }

    private void stubValidUserAndAdmin() {
        when(userService.getAllUsers()).thenReturn(
            List.of(new UserDTO(USER_ID, "name", "user@test.com", UserGroupDiscount.NONE)));
        when(systemAdminService.getAllUsers()).thenReturn(
            List.of(new UserInfo(USER_ID, "name", "user@test.com", "pass", UserGroupDiscount.NONE)));
    }

    // --- invalid session ---

    @Test
    void WhenGetAllHistoryOrdersByUserGivenInvalidSession_ThenReturnEmptyList() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrdersByUser(INVALID_SESSION, USER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void WhenGetAllHistoryOrdersByUserGivenInvalidSession_ThenRepoNeverCalled() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        historyOrderService.getAllHistoryOrdersByUser(INVALID_SESSION, USER_ID);

        verify(historyOrderRepo, never()).findAllByUserId(any());
    }

    @Test
    void WhenGetAllHistoryOrdersByUserGivenInvalidSession_ThenUserServiceNeverCalled() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        historyOrderService.getAllHistoryOrdersByUser(INVALID_SESSION, USER_ID);

        verify(userService, never()).getAllUsers();
    }

    // --- user not in system ---

    @Test
    void WhenGetAllHistoryOrdersByUserGivenUserNotInSystem_ThenReturnEmptyList() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(userService.getAllUsers()).thenReturn(List.of());

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrdersByUser(VALID_SESSION, USER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void WhenGetAllHistoryOrdersByUserGivenUserNotInSystem_ThenRepoNeverCalled() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(userService.getAllUsers()).thenReturn(List.of());

        historyOrderService.getAllHistoryOrdersByUser(VALID_SESSION, USER_ID);

        verify(historyOrderRepo, never()).findAllByUserId(any());
    }

    // --- user in system but not admin ---

    @Test
    void WhenGetAllHistoryOrdersByUserGivenUserNotAdmin_ThenReturnEmptyList() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(userService.getAllUsers()).thenReturn(
            List.of(new UserDTO(USER_ID, "name", "user@test.com", UserGroupDiscount.NONE)));
        when(systemAdminService.getAllUsers()).thenReturn(List.of());

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrdersByUser(VALID_SESSION, USER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void WhenGetAllHistoryOrdersByUserGivenUserNotAdmin_ThenRepoNeverCalled() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(userService.getAllUsers()).thenReturn(
            List.of(new UserDTO(USER_ID, "name", "user@test.com", UserGroupDiscount.NONE)));
        when(systemAdminService.getAllUsers()).thenReturn(List.of());

        historyOrderService.getAllHistoryOrdersByUser(VALID_SESSION, USER_ID);

        verify(historyOrderRepo, never()).findAllByUserId(any());
    }

    // --- valid user and admin ---

    @Test
    void WhenGetAllHistoryOrdersByUserGivenValidUserAndAdmin_ThenReturnOrders() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        stubValidUserAndAdmin();
        when(historyOrderRepo.findAllByUserId(USER_ID)).thenReturn(List.of(item));

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrdersByUser(VALID_SESSION, USER_ID);

        assertEquals(1, result.size());
    }

    @Test
    void WhenGetAllHistoryOrdersByUserGivenValidUserAndAdmin_ThenReturnCorrectDTOs() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        stubValidUserAndAdmin();
        when(historyOrderRepo.findAllByUserId(USER_ID)).thenReturn(List.of(item));

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrdersByUser(VALID_SESSION, USER_ID);

        assertEquals(ORDER_ID, result.get(0).getOrderId());
        assertEquals(USER_ID, result.get(0).getUserId());
        assertEquals(EVENT_ID, result.get(0).getEventId());
    }

    @Test
    void WhenGetAllHistoryOrdersByUserGivenValidUserAndAdmin_ThenRepoCalledWithCorrectUserId() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        stubValidUserAndAdmin();
        when(historyOrderRepo.findAllByUserId(USER_ID)).thenReturn(List.of());

        historyOrderService.getAllHistoryOrdersByUser(VALID_SESSION, USER_ID);

        verify(historyOrderRepo, times(1)).findAllByUserId(USER_ID);
    }

    @Test
    void WhenGetAllHistoryOrdersByUserGivenValidUserAndAdminNoOrders_ThenReturnEmptyList() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        stubValidUserAndAdmin();
        when(historyOrderRepo.findAllByUserId(USER_ID)).thenReturn(List.of());

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrdersByUser(VALID_SESSION, USER_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void WhenGetAllHistoryOrdersByUserGivenValidUserAndAdminMultipleOrders_ThenReturnAllDTOs() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        stubValidUserAndAdmin();
        HistoryOrderItem item2 = new HistoryOrderItem("order-002", USER_ID, "event-002", COMPANY_ID, 49.99,
            List.of("seat-2"), new HashMap<>());
        when(historyOrderRepo.findAllByUserId(USER_ID)).thenReturn(List.of(item, item2));

        List<HistoryOrderDTO> result = historyOrderService.getAllHistoryOrdersByUser(VALID_SESSION, USER_ID);

        assertEquals(2, result.size());
    }
}
