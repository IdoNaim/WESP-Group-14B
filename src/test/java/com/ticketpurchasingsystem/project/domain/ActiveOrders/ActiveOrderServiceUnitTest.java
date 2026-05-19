package com.ticketpurchasingsystem.project.domain.ActiveOrders;

import com.ticketpurchasingsystem.project.application.ActiveOrderService;
import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.IBarCodeGateway;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith; // Added
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension; // Added

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Added this to initialize the mocks!
public class ActiveOrderServiceUnitTest {

    @Mock private IActiveOrderRepo activeOrderRepoMock;
    @Mock private ActiveOrderPublisher activeOrderPublisherMock;
    @Mock private AuthenticationService authenticationServiceMock;
    @Mock private IBarCodeGateway barcodeGatewayMock;
    @Mock private ActiveOrderListener activeOrderListenerMock;
    @Mock private ActiveOrderHandler activeOrderHandlerMock;

    @InjectMocks
    private ActiveOrderService activeOrderService;

    private static final String VALID_TOKEN   = "valid-token";
    private static final String INVALID_TOKEN = "bad-token";
    private static final String USER_ID       = "user123";
    private static final String OTHER_USER_ID = "user456";
    private static final String ORDER_ID      = "order-001";
    private static final String EVENT_ID      = "event-001";

    private static final SessionToken VALID_SESSION   = new SessionToken(VALID_TOKEN, 9999999999L);
    private static final SessionToken INVALID_SESSION = new SessionToken(INVALID_TOKEN, 9999999999L);

    private ActiveOrderItem orderForUser(String userId) {
        return new ActiveOrderItem(ORDER_ID, userId, EVENT_ID);
    }

    private ActiveOrderItem orderWithSeats(String userId) {
        ActiveOrderItem order = new ActiveOrderItem(ORDER_ID, userId, EVENT_ID);
        order.addSeatIds(List.of("seat-1", "seat-2"));
        return order;
    }
    //---------------------
    //cancelActiveOrder
    //---------------------
    @Test
    void GivenInvalidSession_WhenCancelActiveOrder_ThenThrowRuntimeException() {
        // Arrange
        when(authenticationServiceMock.validate(INVALID_TOKEN)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                activeOrderService.cancelActiveOrder(INVALID_SESSION, USER_ID, ORDER_ID)
        );

        verifyNoInteractions(activeOrderRepoMock, activeOrderHandlerMock);
    }

    @Test
    void GivenOrderNotFound_WhenCancelActiveOrder_ThenThrowIllegalArgumentException() {
        // Arrange
        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                activeOrderService.cancelActiveOrder(VALID_SESSION, USER_ID, ORDER_ID)
        );

        verify(activeOrderHandlerMock, never()).isUsersOrder(any(), any());
        verify(activeOrderRepoMock, never()).markAsProcessing(anyString());
    }

    @Test
    void GivenOrderDoesNotBelongToUser_WhenCancelActiveOrder_ThenThrowIllegalArgumentException() {
        // Arrange
        ActiveOrderItem wrongUsersOrder = orderForUser(OTHER_USER_ID);

        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(wrongUsersOrder);
        when(activeOrderHandlerMock.isUsersOrder(USER_ID, wrongUsersOrder)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                activeOrderService.cancelActiveOrder(VALID_SESSION, USER_ID, ORDER_ID)
        );

        verify(activeOrderRepoMock, never()).markAsProcessing(anyString());
    }

    @Test
    void GivenOrderIsAlreadyProcessing_WhenCancelActiveOrder_ThenThrowIllegalStateException() {
        // Arrange
        ActiveOrderItem validOrder = orderForUser(USER_ID);

        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(validOrder);
        when(activeOrderHandlerMock.isUsersOrder(USER_ID, validOrder)).thenReturn(true);
        when(activeOrderRepoMock.markAsProcessing(ORDER_ID)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                activeOrderService.cancelActiveOrder(VALID_SESSION, USER_ID, ORDER_ID)
        );

        verify(activeOrderRepoMock, never()).delete(anyString());
    }

    @Test
    void GivenValidOrderAndSession_WhenCancelActiveOrder_ThenCancelAndDeleteOrder() {
        // Arrange
        ActiveOrderItem validOrder = orderForUser(USER_ID);

        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(validOrder);
        when(activeOrderHandlerMock.isUsersOrder(USER_ID, validOrder)).thenReturn(true);
        when(activeOrderRepoMock.markAsProcessing(ORDER_ID)).thenReturn(true);

        // Act & Assert
        assertDoesNotThrow(() ->
                activeOrderService.cancelActiveOrder(VALID_SESSION, USER_ID, ORDER_ID)
        );

        verify(activeOrderRepoMock, times(1)).delete(ORDER_ID);
    }
    //--------------------------
    // getActiveOrderInfo
    //--------------------------
    @Test
    void GivenInvalidSession_WhenGetActiveOrderInfo_ThenThrowIllegalArgumentException() {
        // Arrange
        when(authenticationServiceMock.validate(INVALID_TOKEN)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                activeOrderService.getActiveOrderInfo(INVALID_SESSION, ORDER_ID)
        );

        verifyNoInteractions(activeOrderRepoMock, activeOrderHandlerMock);
    }

    @Test
    void GivenOrderNotFound_WhenGetActiveOrderInfo_ThenThrowIllegalArgumentException() {
        // Arrange
        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationServiceMock.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                activeOrderService.getActiveOrderInfo(VALID_SESSION, ORDER_ID)
        );

        verifyNoInteractions(activeOrderHandlerMock);
    }

    @Test
    void GivenOrderBelongsToOtherUser_WhenGetActiveOrderInfo_ThenThrowIllegalArgumentException() {
        // Arrange
        ActiveOrderItem wrongUsersOrder = orderForUser(OTHER_USER_ID);

        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationServiceMock.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(wrongUsersOrder);
        // Explicitly telling the mock handler to return null (denying access)
        when(activeOrderHandlerMock.getActiveOrderInfo(USER_ID, wrongUsersOrder)).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                activeOrderService.getActiveOrderInfo(VALID_SESSION, ORDER_ID)
        );
    }

    @Test
    void GivenNullOrderId_WhenGetActiveOrderInfo_ThenThrowIllegalArgumentException() {
        // Arrange
        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationServiceMock.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(activeOrderRepoMock.findById(null)).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                activeOrderService.getActiveOrderInfo(VALID_SESSION, null)
        );
    }

    @Test
    void GivenValidOrderAndSession_WhenGetActiveOrderInfo_ThenReturnOrderDTO() {
        // Arrange
        ActiveOrderItem validOrder = orderForUser(USER_ID);
        ActiveOrderDTO expectedDTO = new ActiveOrderDTO(validOrder);

        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationServiceMock.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(validOrder);
        when(activeOrderHandlerMock.getActiveOrderInfo(USER_ID, validOrder)).thenReturn(expectedDTO);

        // Act & Assert
        ActiveOrderDTO actualDTO = assertDoesNotThrow(() ->
                activeOrderService.getActiveOrderInfo(VALID_SESSION, ORDER_ID)
        );

        assertNotNull(actualDTO);
        assertEquals(expectedDTO, actualDTO);
    }
    //------------------
    //
    //------------------
}