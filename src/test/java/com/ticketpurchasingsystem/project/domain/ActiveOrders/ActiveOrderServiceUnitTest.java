package com.ticketpurchasingsystem.project.domain.ActiveOrders;

import com.ticketpurchasingsystem.project.application.ActiveOrderService;
import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.IBarCodeGateway;
import com.ticketpurchasingsystem.project.application.IPaymentGateway;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith; // Added
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension; // Added

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
    private static final String AREA_ID = "standing-zone-A";
    private static final int QUANTITY = 3;
    private static final double AMOUNT = 100.0;

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
    // createPendingOrder
    //------------------
    @Test
    void GivenValidOrderDetails_WhenCreatePendingOrder_ThenReturnCorrectOrderDetails() {
        // Arrange
        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findByUserId(USER_ID)).thenReturn(null);
        // Assuming your internal isValidEventID method depends on this publisher call:
        when(activeOrderPublisherMock.publishIsValidEventIDEvent(EVENT_ID)).thenReturn(true);
        when(activeOrderHandlerMock.canCreateActiveOrder(any(ActiveOrderItem.class))).thenReturn(true);

        // Act
        ActiveOrderItem order = activeOrderService.createPendingOrder(VALID_SESSION, USER_ID, EVENT_ID);

        // Assert
        assertNotNull(order);
        assertEquals(EVENT_ID, order.getEventId());
        assertEquals(USER_ID, order.getUserId());
        assertNotNull(order.getOrderId());
    }

    @Test
    void GivenValidOrderDetails_WhenCreatePendingOrder_ThenOrderIsSavedInRepo() {
        // Arrange
        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findByUserId(USER_ID)).thenReturn(null);
        when(activeOrderPublisherMock.publishIsValidEventIDEvent(EVENT_ID)).thenReturn(true);
        when(activeOrderHandlerMock.canCreateActiveOrder(any(ActiveOrderItem.class))).thenReturn(true);

        // Act
        ActiveOrderItem order = activeOrderService.createPendingOrder(VALID_SESSION, USER_ID, EVENT_ID);

        // Assert
        verify(activeOrderRepoMock, times(1)).save(order);
    }

    @Test
    void GivenInvalidSessionToken_WhenCreatePendingOrder_ThenThrowRuntimeException() {
        // Arrange
        when(authenticationServiceMock.validate(INVALID_TOKEN)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                activeOrderService.createPendingOrder(INVALID_SESSION, USER_ID, EVENT_ID)
        );
        verifyNoInteractions(activeOrderRepoMock, activeOrderHandlerMock);
    }

    @Test
    void GivenExistingActiveOrderForUser_WhenCreatePendingOrder_ThenThrowIllegalArgumentException() {
        // Arrange
        ActiveOrderItem existingOrder = orderForUser(USER_ID);
        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findByUserId(USER_ID)).thenReturn(existingOrder);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                activeOrderService.createPendingOrder(VALID_SESSION, USER_ID, EVENT_ID)
        );
        verify(activeOrderRepoMock, never()).save(any());
    }

    @Test
    void GivenInvalidEventId_WhenCreatePendingOrder_ThenThrowRuntimeException() {
        // Arrange
        String badEventId = "invalid-event-999";
        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findByUserId(USER_ID)).thenReturn(null);
        when(activeOrderPublisherMock.publishIsValidEventIDEvent(badEventId)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                activeOrderService.createPendingOrder(VALID_SESSION, USER_ID, badEventId)
        );
        verify(activeOrderRepoMock, never()).save(any());
    }

    @Test
    void GivenHandlerDeniesCreation_WhenCreatePendingOrder_ThenThrowRuntimeException() {
        // Arrange
        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findByUserId(USER_ID)).thenReturn(null);
        when(activeOrderPublisherMock.publishIsValidEventIDEvent(EVENT_ID)).thenReturn(true);
        // Handler returns false (business rules block creation)
        when(activeOrderHandlerMock.canCreateActiveOrder(any(ActiveOrderItem.class))).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                activeOrderService.createPendingOrder(VALID_SESSION, USER_ID, EVENT_ID)
        );
        verify(activeOrderRepoMock, never()).save(any());
    }

    // --- CONCURRENCY INTEGRATION TESTS ---

//    @Test
//    void GivenTwoDifferentUsers_WhenCreatePendingOrderConcurrently_ThenBothSucceedWithUniqueOrderIds() throws InterruptedException {
//        ActiveOrderMemRepo realRepo = new ActiveOrderMemRepo();
//        ActiveOrderService service = new ActiveOrderService(
//                new ActiveOrderListener(realRepo), activeOrderPublisherMock, realRepo, activeOrderHandlerMock,
//                authenticationServiceMock, barcodeGatewayMock
//        );
//
//        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
//        when(activeOrderPublisherMock.publishIsValidEventIDEvent(any())).thenReturn(true);
//        when(activeOrderHandlerMock.canCreateActiveOrder(any(ActiveOrderItem.class))).thenReturn(true);
//
//        CountDownLatch startLatch = new CountDownLatch(1);
//        CountDownLatch doneLatch = new CountDownLatch(2);
//        List<ActiveOrderItem> results = Collections.synchronizedList(new ArrayList<>());
//        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());
//
//        Runnable task = (userId) -> {
//            try {
//                startLatch.await();
//                results.add(service.createPendingOrder(VALID_SESSION, userId, EVENT_ID));
//            } catch (Exception e) { errors.add(e); }
//            finally { doneLatch.countDown(); }
//        };
//
//        new Thread(() -> task.run("userA")).start();
//        new Thread(() -> task.run("userB")).start();
//
//        startLatch.countDown();
//        doneLatch.await();
//
//        assertTrue(errors.isEmpty());
//        assertEquals(2, results.size());
//        assertNotEquals(results.get(0).getOrderId(), results.get(1).getOrderId());
//    }
//
//    @Test
//    void GivenSameUser_WhenCreatePendingOrderTwiceConcurrently_ThenOnlyOneSucceeds() throws InterruptedException {
//        ActiveOrderMemRepo realRepo = new ActiveOrderMemRepo();
//        ActiveOrderService service = new ActiveOrderService(
//                new ActiveOrderListener(realRepo), activeOrderPublisherMock, realRepo, activeOrderHandlerMock,
//                authenticationServiceMock, barcodeGatewayMock
//        );
//
//        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
//        when(activeOrderPublisherMock.publishIsValidEventIDEvent(any())).thenReturn(true);
//        when(activeOrderHandlerMock.canCreateActiveOrder(any(ActiveOrderItem.class))).thenReturn(true);
//
//        CountDownLatch startLatch = new CountDownLatch(1);
//        CountDownLatch doneLatch = new CountDownLatch(2);
//        AtomicInteger successCount = new AtomicInteger(0);
//        AtomicInteger failCount = new AtomicInteger(0);
//
//        for (int i = 0; i < 2; i++) {
//            new Thread(() -> {
//                try {
//                    startLatch.await();
//                    service.createPendingOrder(VALID_SESSION, USER_ID, EVENT_ID);
//                    successCount.incrementAndGet();
//                } catch (Exception e) {
//                    failCount.incrementAndGet();
//                } finally { doneLatch.countDown(); }
//            }).start();
//        }
//
//        startLatch.countDown();
//        doneLatch.await();
//
//        assertEquals(1, successCount.get());
//        assertEquals(1, failCount.get());
//        assertNotNull(realRepo.findByUserId(USER_ID));
//    }
//
//    @Test
//    void GivenNDifferentUsers_WhenCreatePendingOrderConcurrently_ThenAllSucceedWithUniqueOrderIds() throws InterruptedException {
//        int numberOfUsers = 20;
//        ActiveOrderMemRepo realRepo = new ActiveOrderMemRepo();
//        ActiveOrderService service = new ActiveOrderService(
//                new ActiveOrderListener(realRepo), activeOrderPublisherMock, realRepo, activeOrderHandlerMock,
//                authenticationServiceMock, barcodeGatewayMock
//        );
//
//        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
//        when(activeOrderPublisherMock.publishIsValidEventIDEvent(any())).thenReturn(true);
//        when(activeOrderHandlerMock.canCreateActiveOrder(any(ActiveOrderItem.class))).thenReturn(true);
//
//        ExecutorService executor = Executors.newFixedThreadPool(numberOfUsers);
//        CountDownLatch startLatch = new CountDownLatch(1);
//        CountDownLatch doneLatch = new CountDownLatch(numberOfUsers);
//        List<ActiveOrderItem> results = Collections.synchronizedList(new ArrayList<>());
//        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());
//
//        for (int i = 0; i < numberOfUsers; i++) {
//            final String multiUserId = "user" + i;
//            executor.submit(() -> {
//                try {
//                    startLatch.await();
//                    results.add(service.createPendingOrder(VALID_SESSION, multiUserId, EVENT_ID));
//                } catch (Exception e) { errors.add(e); }
//                finally { doneLatch.countDown(); }
//            });
//        }
//
//        startLatch.countDown();
//        doneLatch.await();
//        executor.shutdown();
//
//        assertTrue(errors.isEmpty());
//        assertEquals(numberOfUsers, results.size());
//        long uniqueOrderIds = results.stream().map(ActiveOrderItem::getOrderId).distinct().count();
//        assertEquals(numberOfUsers, uniqueOrderIds);
//    }
    //--------------
    // addSeatsToActiveOrder
    //--------------
@Test
void GivenInvalidSession_WhenAddSeatsToActiveOrder_ThenThrowRuntimeException() {
    // Arrange
    List<String> seats = List.of("seat-1", "seat-2");
    when(authenticationServiceMock.validate(INVALID_TOKEN)).thenReturn(false);

    // Act & Assert
    assertThrows(RuntimeException.class, () ->
            activeOrderService.addSeatsToActiveOrder(INVALID_SESSION, ORDER_ID, seats)
    );
    verifyNoInteractions(activeOrderRepoMock, activeOrderHandlerMock, activeOrderPublisherMock);
}

    @Test
    void GivenOrderNotFound_WhenAddSeatsToActiveOrder_ThenThrowIllegalArgumentException() {
        // Arrange
        List<String> seats = List.of("seat-1", "seat-2");
        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                activeOrderService.addSeatsToActiveOrder(VALID_SESSION, ORDER_ID, seats)
        );
        verifyNoInteractions(activeOrderHandlerMock, activeOrderPublisherMock);
    }

    @Test
    void GivenSeatsCannotBeReserved_WhenAddSeatsToActiveOrder_ThenThrowIllegalStateException() {
        // Arrange
        List<String> requestedSeats = List.of("seat-1", "seat-2");
        ActiveOrderItem validOrder = orderForUser(USER_ID); // Assuming helper sets valid timestamp/not expired

        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(validOrder);
        when(activeOrderHandlerMock.getSeatsToReserve(validOrder.getSeatIds(), requestedSeats)).thenReturn(requestedSeats);
        // Publisher fails to reserve the seats on the bus/broker
        when(activeOrderPublisherMock.publishReserveSeats(EVENT_ID, requestedSeats)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                activeOrderService.addSeatsToActiveOrder(VALID_SESSION, ORDER_ID, requestedSeats)
        );
        verify(activeOrderRepoMock, never()).update(any());
    }

    @Test
    void GivenHandlerFailsToModifyOrder_WhenAddSeatsToActiveOrder_ThenThrowRuntimeException() {
        // Arrange
        List<String> requestedSeats = List.of("seat-1", "seat-2");
        ActiveOrderItem validOrder = orderForUser(USER_ID);

        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(validOrder);
        when(activeOrderHandlerMock.getSeatsToReserve(validOrder.getSeatIds(), requestedSeats)).thenReturn(requestedSeats);
        when(activeOrderPublisherMock.publishReserveSeats(EVENT_ID, requestedSeats)).thenReturn(true);
        // Handler returns null meaning adding seats failed business rules
        when(activeOrderHandlerMock.addSeatsToActiveOrder(validOrder, requestedSeats)).thenReturn(null);
        when(activeOrderHandlerMock.canReleaseSeats(requestedSeats)).thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                activeOrderService.addSeatsToActiveOrder(VALID_SESSION, ORDER_ID, requestedSeats)
        );
        verify(activeOrderPublisherMock, times(1)).publishReleaseSeats(validOrder.getEventId(), requestedSeats);
        verify(activeOrderRepoMock, never()).update(any());
    }

    @Test
    void GivenDatabaseErrorOnUpdate_WhenAddSeatsToActiveOrder_ThenTriggerRollback() {
        // Arrange
        List<String> requestedSeats = List.of("seat-1", "seat-2");
        ActiveOrderItem validOrder = orderForUser(USER_ID);
        ActiveOrderItem updatedOrder = orderWithSeats(USER_ID);

        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(validOrder);
        when(activeOrderHandlerMock.getSeatsToReserve(validOrder.getSeatIds(), requestedSeats)).thenReturn(requestedSeats);
        when(activeOrderPublisherMock.publishReserveSeats(EVENT_ID, requestedSeats)).thenReturn(true);
        when(activeOrderHandlerMock.addSeatsToActiveOrder(validOrder, requestedSeats)).thenReturn(updatedOrder);
        when(activeOrderHandlerMock.canReleaseSeats(requestedSeats)).thenReturn(true);
        // Force the database to crash on update
        doThrow(new RuntimeException("Database down")).when(activeOrderRepoMock).update(updatedOrder);

        // Act & Assert
        // Note: The service catches this exception internally, logs it, and swallows it after rolling back.
        assertDoesNotThrow(() ->
                activeOrderService.addSeatsToActiveOrder(VALID_SESSION, ORDER_ID, requestedSeats)
        );
        verify(activeOrderPublisherMock, times(1)).publishReleaseSeats(validOrder.getEventId(), requestedSeats);
    }

    @Test
    void GivenValidInputsAndAvailableSeats_WhenAddSeatsToActiveOrder_ThenSeatsAreSuccessfullyAddedAndSaved() {
        // Arrange
        List<String> requestedSeats = List.of("seat-1", "seat-2");
        ActiveOrderItem validOrder = orderForUser(USER_ID);
        ActiveOrderItem updatedOrder = orderWithSeats(USER_ID);

        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(validOrder);
        when(activeOrderHandlerMock.getSeatsToReserve(validOrder.getSeatIds(), requestedSeats)).thenReturn(requestedSeats);
        when(activeOrderPublisherMock.publishReserveSeats(EVENT_ID, requestedSeats)).thenReturn(true);
        when(activeOrderHandlerMock.addSeatsToActiveOrder(validOrder, requestedSeats)).thenReturn(updatedOrder);

        // Act & Assert
        assertDoesNotThrow(() ->
                activeOrderService.addSeatsToActiveOrder(VALID_SESSION, ORDER_ID, requestedSeats)
        );

        // Verify that repo update ran smoothly with the modified order object
        verify(activeOrderRepoMock, times(1)).update(updatedOrder);
        verify(activeOrderPublisherMock, times(1)).publishReserveSeats(validOrder.getEventId(), requestedSeats);
    }
    //----------------
    // addStandingAreaToActiveOrder
    //----------------
    @Test
    void GivenInvalidSession_WhenAddStandingAreaToActiveOrder_ThenThrowRuntimeException() {
        // Arrange
        when(authenticationServiceMock.validate(INVALID_TOKEN)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                activeOrderService.addStandingAreaToActiveOrder(INVALID_SESSION, ORDER_ID, AREA_ID, QUANTITY)
        );
        verifyNoInteractions(activeOrderRepoMock, activeOrderHandlerMock, activeOrderPublisherMock);
    }

    @Test
    void GivenOrderNotFound_WhenAddStandingAreaToActiveOrder_ThenThrowIllegalArgumentException() {
        // Arrange
        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                activeOrderService.addStandingAreaToActiveOrder(VALID_SESSION, ORDER_ID, AREA_ID, QUANTITY)
        );
        verifyNoInteractions(activeOrderHandlerMock, activeOrderPublisherMock);
    }

    @Test
    void GivenStandingAreaCannotBeReserved_WhenAddStandingAreaToActiveOrder_ThenThrowIllegalStateException() {
        // Arrange
        ActiveOrderItem validOrder = orderForUser(USER_ID);

        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(validOrder);
        // Publisher fails to block the inventory allotment
        when(activeOrderPublisherMock.publishReserveStandingArea(EVENT_ID, AREA_ID, QUANTITY)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                activeOrderService.addStandingAreaToActiveOrder(VALID_SESSION, ORDER_ID, AREA_ID, QUANTITY)
        );
        verify(activeOrderRepoMock, never()).update(any());
    }

    @Test
    void GivenHandlerFailsToModifyOrder_WhenAddStandingAreaToActiveOrder_ThenThrowRuntimeException() {
        // Arrange
        ActiveOrderItem validOrder = orderForUser(USER_ID);

        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(validOrder);
        when(activeOrderPublisherMock.publishReserveStandingArea(EVENT_ID, AREA_ID, QUANTITY)).thenReturn(true);
        // Business logic handler rejects the modification layout
        when(activeOrderHandlerMock.addStandingAreaToActiveOrder(validOrder, AREA_ID, QUANTITY)).thenReturn(null);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                activeOrderService.addStandingAreaToActiveOrder(VALID_SESSION, ORDER_ID, AREA_ID, QUANTITY)
        );
        verify(activeOrderPublisherMock, times(1)).publishReleaseStandingArea(validOrder.getEventId(), AREA_ID, QUANTITY);
        verify(activeOrderRepoMock, never()).update(any());
    }

    @Test
    void GivenDatabaseErrorOnUpdate_WhenAddStandingAreaToActiveOrder_ThenTriggerRollback() {
        // Arrange
        ActiveOrderItem validOrder = orderForUser(USER_ID);
        ActiveOrderItem updatedOrder = orderForUser(USER_ID); // Mocking returned modified object

        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(validOrder);
        when(activeOrderPublisherMock.publishReserveStandingArea(EVENT_ID, AREA_ID, QUANTITY)).thenReturn(true);
        when(activeOrderHandlerMock.addStandingAreaToActiveOrder(validOrder, AREA_ID, QUANTITY)).thenReturn(updatedOrder);

        // Force database execution layer exception
        doThrow(new RuntimeException("DB Connection Timeout")).when(activeOrderRepoMock).update(updatedOrder);

        // Act & Assert
        // The catch block swallows the exception after running the message pipeline fallback
        assertDoesNotThrow(() ->
                activeOrderService.addStandingAreaToActiveOrder(VALID_SESSION, ORDER_ID, AREA_ID, QUANTITY)
        );

        // Verify the rollback matches the correct Event ID parameter matching code execution path
        verify(activeOrderPublisherMock, times(1)).publishReleaseStandingArea(EVENT_ID, AREA_ID, QUANTITY);
    }

    @Test
    void GivenValidInputsAndAvailableInventory_WhenAddStandingAreaToActiveOrder_ThenStandingAreaIsSuccessfullyAdded() {
        // Arrange
        ActiveOrderItem validOrder = orderForUser(USER_ID);
        ActiveOrderItem updatedOrder = orderForUser(USER_ID);

        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(validOrder);
        when(activeOrderPublisherMock.publishReserveStandingArea(EVENT_ID, AREA_ID, QUANTITY)).thenReturn(true);
        when(activeOrderHandlerMock.addStandingAreaToActiveOrder(validOrder, AREA_ID, QUANTITY)).thenReturn(updatedOrder);

        // Act & Assert
        assertDoesNotThrow(() ->
                activeOrderService.addStandingAreaToActiveOrder(VALID_SESSION, ORDER_ID, AREA_ID, QUANTITY)
        );

        verify(activeOrderRepoMock, times(1)).update(updatedOrder);
    }
    //---------
    //completeOrder
    //------------
    @Test
    void GivenValidOrderAndPayment_WhenCompleteOrder_ThenOrderIsRemovedFromRepo() {
        // Arrange
        ActiveOrderItem validOrder = orderForUser(USER_ID);
        IPaymentGateway paymentGatewayMock = mock(IPaymentGateway.class);

        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(validOrder);
        when(activeOrderPublisherMock.publishIsUpToPolicy(any())).thenReturn(true);
        when(activeOrderRepoMock.markAsProcessing(ORDER_ID)).thenReturn(true);
        when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(List.of(new BarcodeDTO("barcode")));

        // Stubbing the internal payment wrapper context (adjust based on your actual internal gateway call if necessary)
        // If your internal helper method calls paymentGatewayMock.execute() or similar, stub that here.
        // Assuming your helper method matches the setup:
        when(paymentGatewayMock.pay()).thenReturn(true);

        // Act
        List<BarcodeDTO> result = activeOrderService.completeOrder(paymentGatewayMock, VALID_SESSION, AMOUNT, ORDER_ID);

        // Assert
        assertNotNull(result);
        verify(activeOrderRepoMock, times(1)).delete(ORDER_ID);
        verify(activeOrderPublisherMock, times(1)).publishCompletedOrder(any(), eq(AMOUNT));
    }
    @Test
    void GivenValidOrderAndPayment_WhenCompleteOrder_ThenOrderIsRemovedAndPublished() {
        // Arrange
        ActiveOrderItem validOrder = orderForUser(USER_ID);
        IPaymentGateway paymentGatewayMock = mock(IPaymentGateway.class);

        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(validOrder);
        when(activeOrderPublisherMock.publishIsUpToPolicy(any())).thenReturn(true);
        when(activeOrderRepoMock.markAsProcessing(ORDER_ID)).thenReturn(true);
        when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(List.of(new BarcodeDTO("barcode")));
        when(paymentGatewayMock.pay()).thenReturn(true);

        // Act
        List<BarcodeDTO> result = activeOrderService.completeOrder(paymentGatewayMock, VALID_SESSION, AMOUNT, ORDER_ID);

        // Assert
        assertNotNull(result);

        verify(activeOrderRepoMock, times(1)).delete(ORDER_ID);
        verify(activeOrderPublisherMock, times(1)).publishCompletedOrder(any(ActiveOrderDTO.class), eq(AMOUNT));
    }

    @Test
    void GivenExpiredSessionToken_WhenCompleteOrder_ThenThrowRuntimeException() {
        // Arrange
        IPaymentGateway paymentGatewayMock = mock(IPaymentGateway.class);
        when(authenticationServiceMock.validate(INVALID_TOKEN)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                activeOrderService.completeOrder(paymentGatewayMock, INVALID_SESSION, AMOUNT, ORDER_ID)
        );
        verify(activeOrderRepoMock, never()).delete(anyString());
    }

    @Test
    void GivenNonExistingOrder_WhenCompleteOrder_ThenThrowIllegalArgumentException() {
        // Arrange
        IPaymentGateway paymentGatewayMock = mock(IPaymentGateway.class);
        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                activeOrderService.completeOrder(paymentGatewayMock, VALID_SESSION, AMOUNT, ORDER_ID)
        );
        verify(activeOrderRepoMock, never()).delete(anyString());
    }

    @Test
    void GivenExpiredOrder_WhenCompleteOrder_ThenThrowExceptionAndRollback() {
        // Arrange
        IPaymentGateway paymentGatewayMock = mock(IPaymentGateway.class);
        ActiveOrderItem expiredOrder = orderForUser(USER_ID);
        expiredOrder.setSeatIds(List.of("A-1", "A-2"));

        HashMap<String, Integer> standingArea = new HashMap<>();
        standingArea.put("GA-1", 3);
        expiredOrder.setStandingAreaQuantities(standingArea);

        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(expiredOrder);

        // FIX: Stub the handler to explicitly mark this order as expired
        when(activeOrderHandlerMock.isOrderExpired(expiredOrder)).thenReturn(true);

        // Stub the fallback guards to allow the rollback to execute
        when(activeOrderHandlerMock.canReleaseSeats(any())).thenReturn(true);
        when(activeOrderHandlerMock.canReleaseStanding(any())).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                activeOrderService.completeOrder(paymentGatewayMock, VALID_SESSION, AMOUNT, ORDER_ID)
        );

        // Verify Rollback
        verify(activeOrderPublisherMock, times(1)).publishReleaseSeats(EVENT_ID, List.of("A-1", "A-2"));
        verify(activeOrderPublisherMock, times(1)).publishReleaseStandingArea(EVENT_ID, "GA-1", 3);
        verify(activeOrderRepoMock, times(1)).delete(ORDER_ID);
    }

    @Test
    void GivenPaymentFails_WhenCompleteOrder_ThenThrowIllegalStateExceptionAndRollback() {
        // Arrange
        IPaymentGateway paymentGatewayMock = mock(IPaymentGateway.class);
        ActiveOrderItem validOrder = orderForUser(USER_ID);
        validOrder.setSeatIds(List.of("B-10", "B-11"));

        HashMap<String, Integer> standingArea = new HashMap<>();
        standingArea.put("VIP-1", 2);
        validOrder.setStandingAreaQuantities(standingArea);

        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(validOrder);
        when(activeOrderPublisherMock.publishIsUpToPolicy(any())).thenReturn(true);
        when(activeOrderRepoMock.markAsProcessing(ORDER_ID)).thenReturn(true);
        when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(List.of(new BarcodeDTO("barcode")));
        when(paymentGatewayMock.pay()).thenReturn(false);

        // FIX: Stub the handler to allow the rollback logic to pass its guards
        when(activeOrderHandlerMock.canReleaseSeats(validOrder.getSeatIds())).thenReturn(true);
        when(activeOrderHandlerMock.canReleaseStanding(validOrder.getStandingAreaQuantities())).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                activeOrderService.completeOrder(paymentGatewayMock, VALID_SESSION, AMOUNT, ORDER_ID)
        );

        verify(activeOrderPublisherMock, times(1)).publishReleaseSeats(EVENT_ID, List.of("B-10", "B-11"));
        verify(activeOrderPublisherMock, times(1)).publishReleaseStandingArea(EVENT_ID, "VIP-1", 2);
        verify(activeOrderRepoMock, times(1)).delete(ORDER_ID);
        verify(activeOrderPublisherMock, never()).publishCompletedOrder(any(), anyDouble());
    }

    @Test
    void GivenBarcodeGenerationFails_WhenCompleteOrder_ThenThrowIllegalStateExceptionAndRollback() {
        // Arrange
        IPaymentGateway paymentGatewayMock = mock(IPaymentGateway.class);
        ActiveOrderItem validOrder = orderForUser(USER_ID);
        validOrder.setSeatIds(List.of("C-1"));

        when(authenticationServiceMock.validate(VALID_TOKEN)).thenReturn(true);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(validOrder);
        when(activeOrderPublisherMock.publishIsUpToPolicy(any())).thenReturn(true);
        when(activeOrderRepoMock.markAsProcessing(ORDER_ID)).thenReturn(true);
        when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(null);

        // FIX: Stub the handler to allow the rollback logic to pass its guards
        when(activeOrderHandlerMock.canReleaseSeats(validOrder.getSeatIds())).thenReturn(true);
        when(activeOrderHandlerMock.canReleaseStanding(validOrder.getStandingAreaQuantities())).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                activeOrderService.completeOrder(paymentGatewayMock, VALID_SESSION, AMOUNT, ORDER_ID)
        );

        verify(activeOrderPublisherMock, times(1)).publishReleaseSeats(EVENT_ID, List.of("C-1"));
        verify(activeOrderRepoMock, times(1)).delete(ORDER_ID);
        verify(activeOrderPublisherMock, never()).publishCompletedOrder(any(), anyDouble());
        verifyNoInteractions(paymentGatewayMock);
    }

//    // --- CONCURRENCY INTEGRATION TESTS ---
//
//    @Test
//    void GivenSameOrder_WhenCompleteOrderTwiceConcurrently_ThenOnlyOneSucceedsAndUserChargedOnce() throws InterruptedException {
//        ActiveOrderMemRepo realRepo = new ActiveOrderMemRepo();
//        ActiveOrderService service = new ActiveOrderService(
//                new ActiveOrderListener(realRepo), activeOrderPublisherMock, realRepo, activeOrderHandlerMock,
//                authenticationServiceMock, barcodeGatewayMock);
//
//        IPaymentGateway paymentGatewayMock = mock(IPaymentGateway.class);
//
//        when(authenticationServiceMock.validate("valid-token")).thenReturn(true);
//        when(activeOrderPublisherMock.publishIsValidEventIDEvent(any())).thenReturn(true);
//        when(activeOrderPublisherMock.publishIsUpToPolicy(any())).thenReturn(true);
//        when(paymentGatewayMock.pay()).thenReturn(true);
//        when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(List.of(mock(BarcodeDTO.class)));
//
//        ActiveOrderItem order = service.createPendingOrder(VALID_SESSION, "userA", EVENT_ID);
//        String liveOrderId = order.getOrderId();
//
//        CountDownLatch startLatch = new CountDownLatch(1);
//        CountDownLatch doneLatch = new CountDownLatch(2);
//        AtomicInteger successCount = new AtomicInteger(0);
//        AtomicInteger failCount = new AtomicInteger(0);
//
//        Runnable completionTask = () -> {
//            try {
//                startLatch.await();
//                service.completeOrder(paymentGatewayMock, VALID_SESSION, AMOUNT, liveOrderId);
//                successCount.incrementAndGet();
//            } catch (Exception e) {
//                failCount.incrementAndGet();
//            } finally {
//                doneLatch.countDown();
//            }
//        };
//
//        new Thread(completionTask).start();
//        new Thread(completionTask).start();
//
//        startLatch.countDown();
//        doneLatch.await();
//
//        assertEquals(1, successCount.get());
//        assertEquals(1, failCount.get());
//        verify(paymentGatewayMock, times(1)).pay();
//        assertNull(realRepo.findById(liveOrderId));
//    }
//
//    @Test
//    void GivenSameOrder_WhenCompleteAndCancelConcurrently_ThenOnlyOneSucceedsAndOrderRemoved() throws InterruptedException {
//        ActiveOrderMemRepo realRepo = new ActiveOrderMemRepo();
//        ActiveOrderService service = new ActiveOrderService(
//                new ActiveOrderListener(realRepo), activeOrderPublisherMock, realRepo, activeOrderHandlerMock,
//                authenticationServiceMock, barcodeGatewayMock);
//
//        IPaymentGateway paymentGatewayMock = mock(IPaymentGateway.class);
//
//        when(authenticationServiceMock.validate("valid-token")).thenReturn(true);
//        when(activeOrderPublisherMock.publishIsValidEventIDEvent(any())).thenReturn(true);
//        when(activeOrderPublisherMock.publishIsUpToPolicy(any())).thenReturn(true);
//        when(paymentGatewayMock.pay()).thenReturn(true);
//        when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(List.of(mock(BarcodeDTO.class)));
//        when(activeOrderHandlerMock.isUsersOrder(any(), any())).thenReturn(true);
//
//        ActiveOrderItem order = service.createPendingOrder(VALID_SESSION, "userB", EVENT_ID);
//        String liveOrderId = order.getOrderId();
//
//        CountDownLatch startLatch = new CountDownLatch(1);
//        CountDownLatch doneLatch = new CountDownLatch(2);
//        AtomicInteger successCount = new AtomicInteger(0);
//        AtomicInteger failCount = new AtomicInteger(0);
//
//        new Thread(() -> {
//            try {
//                startLatch.await();
//                service.completeOrder(paymentGatewayMock, VALID_SESSION, AMOUNT, liveOrderId);
//                successCount.incrementAndGet();
//            } catch (Exception e) { failCount.incrementAndGet(); }
//            finally { doneLatch.countDown(); }
//        }).start();
//
//        new Thread(() -> {
//            try {
//                startLatch.await();
//                service.cancelActiveOrder(VALID_SESSION, "userB", liveOrderId);
//                successCount.incrementAndGet();
//            } catch (Exception e) { failCount.incrementAndGet(); }
//            finally { doneLatch.countDown(); }
//        }).start();
//
//        startLatch.countDown();
//        doneLatch.await();
//
//        assertEquals(1, successCount.get());
//        assertEquals(1, failCount.get());
//        assertNull(realRepo.findById(liveOrderId));
//        verify(paymentGatewayMock, atMostOnce()).pay();
//    }
//
//    @Test
//    void GivenNDifferentOrders_WhenCompleteOrderConcurrently_ThenAllSucceedAndAllOrdersRemoved() throws InterruptedException {
//        int orderCount = 20;
//        ActiveOrderMemRepo realRepo = new ActiveOrderMemRepo();
//        ActiveOrderService service = new ActiveOrderService(
//                new ActiveOrderListener(realRepo), activeOrderPublisherMock, realRepo, activeOrderHandlerMock,
//                authenticationServiceMock, barcodeGatewayMock);
//
//        IPaymentGateway paymentGatewayMock = mock(IPaymentGateway.class);
//
//        when(authenticationServiceMock.validate("valid-token")).thenReturn(true);
//        when(activeOrderPublisherMock.publishIsValidEventIDEvent(any())).thenReturn(true);
//        when(activeOrderPublisherMock.publishIsUpToPolicy(any())).thenReturn(true);
//        when(paymentGatewayMock.pay()).thenReturn(true);
//        when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(List.of(mock(BarcodeDTO.class)));
//
//        List<String> generatedOrderIds = Collections.synchronizedList(new ArrayList<>());
//        for (int i = 0; i < orderCount; i++) {
//            ActiveOrderItem o = service.createPendingOrder(VALID_SESSION, "user" + i, EVENT_ID);
//            generatedOrderIds.add(o.getOrderId());
//        }
//
//        ExecutorService executor = Executors.newFixedThreadPool(orderCount);
//        CountDownLatch startLatch = new CountDownLatch(1);
//        CountDownLatch doneLatch = new CountDownLatch(orderCount);
//        AtomicInteger successCount = new AtomicInteger(0);
//        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());
//
//        for (String liveOrderId : generatedOrderIds) {
//            executor.submit(() -> {
//                try {
//                    startLatch.await();
//                    service.completeOrder(paymentGatewayMock, VALID_SESSION, AMOUNT, liveOrderId);
//                    successCount.incrementAndGet();
//                } catch (Exception e) { errors.add(e); }
//                finally { doneLatch.countDown(); }
//            });
//        }
//
//        startLatch.countDown();
//        doneLatch.await();
//        executor.shutdown();
//
//        assertEquals(orderCount, successCount.get());
//        assertTrue(errors.isEmpty());
//        for (String liveOrderId : generatedOrderIds) {
//            assertNull(realRepo.findById(liveOrderId));
//        }
//        verify(paymentGatewayMock, times(orderCount)).pay();
//    }

}
