package com.ticketpurchasingsystem.project.domain.ActiveOrders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.concurrent.CountDownLatch;
import java.util.ArrayList;
import java.util.Collections;

import com.ticketpurchasingsystem.project.application.IPaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ticketpurchasingsystem.project.application.ActiveOrderService;
import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import com.ticketpurchasingsystem.project.infrastructure.ActiveOrderMemRepo;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

import com.ticketpurchasingsystem.project.application.IBarCodeGateway;

public class ActiveOrderTests {
    private IActiveOrderRepo activeOrderRepoMock;
    
    // Changed to concrete class so we can access all specific methods 
    // without needing them to be in the IActiveOrderService interface during testing
    private ActiveOrderService activeOrderService; 
    
    private ActiveOrderPublisher activeOrderPublisher;
    private AuthenticationService authenticationService;
    private IBarCodeGateway barcodeGatewayMock;
    private ActiveOrderHandler activeOrderHandler;

    private static final String VALID_TOKEN   = "valid-token";
    private static final String INVALID_TOKEN = "bad-token";
    private static final String USER_ID       = "user123";
    private static final String OTHER_USER_ID = "user456";
    private static final String ORDER_ID      = "order-001";
    private static final String EVENT_ID      = "event-001";

    private static final SessionToken VALID_SESSION   = new SessionToken(VALID_TOKEN, 9999999999L);
    private static final SessionToken INVALID_SESSION = new SessionToken(INVALID_TOKEN, 9999999999L);

    @BeforeEach
    public void setUp() {
        activeOrderRepoMock = mock(IActiveOrderRepo.class);
        activeOrderPublisher = mock(ActiveOrderPublisher.class);
        authenticationService = mock(AuthenticationService.class);
        barcodeGatewayMock = mock(IBarCodeGateway.class);

        activeOrderHandler = new ActiveOrderHandler();
        activeOrderService = new ActiveOrderService(
            new ActiveOrderListener(activeOrderRepoMock), 
            activeOrderPublisher, 
            activeOrderRepoMock,
            activeOrderHandler,
            authenticationService, 
            barcodeGatewayMock
        );
    }
    /**
       helper functions
     */
    private ActiveOrderItem orderForUser(String userId) {
        return new ActiveOrderItem(ORDER_ID, userId, EVENT_ID);
    }

    private ActiveOrderItem orderWithSeats(String userId) {
        ActiveOrderItem order = new ActiveOrderItem(ORDER_ID, userId, EVENT_ID);
        order.addSeatIds(List.of("seat-1", "seat-2"));
        return order;
    }

    @Test
    public void GivenValidOrder_WhenSaveOrder_thenReturnTrue() {
        ActiveOrderItem activeOrder = new ActiveOrderItem("1", "1","1");
        when(activeOrderRepoMock.save(activeOrder)).thenReturn(true);
        when(activeOrderPublisher.publishIsValidEventIDEvent(activeOrder.getEventId())).thenReturn(true);
        boolean result = activeOrderService.saveOrder(activeOrder);
        assertTrue(result);
    }

    @Test
    public void GivenValidOrder_WhenSaveOrder_thenCallSaveInRepo() {
        ActiveOrderItem activeOrder = new ActiveOrderItem("1", "1","1");
        when(activeOrderRepoMock.save(activeOrder)).thenReturn(true);
        when(activeOrderPublisher.publishIsValidEventIDEvent(activeOrder.getEventId())).thenReturn(true);
        boolean result = activeOrderService.saveOrder(activeOrder);
        verify(activeOrderRepoMock, times(1)).save(activeOrder);
        assertTrue(result);
    }

    @Test
    public void GivenThrownException_WhenSaveOrder_thenReturnFalse() {
        ActiveOrderItem activeOrder = new ActiveOrderItem("1", "1","1");
        when(activeOrderRepoMock.save(activeOrder)).thenThrow(new RuntimeException(" got error when saving order"));
        when(activeOrderPublisher.publishIsValidEventIDEvent(activeOrder.getEventId())).thenReturn(true);
        boolean result = activeOrderService.saveOrder(activeOrder);
        assertFalse(result);
    }

    @Test
    public void GivenNullOrder_WhenSaveOrder_thenReturnFalse() {
        boolean result = activeOrderService.saveOrder(null);
        assertFalse(result);
    }

    @Test
    public void GivenInvalidEventId_WhenSaveOrder_thenReturnErrorMessage() {
        ActiveOrderItem activeOrder = new ActiveOrderItem("1", "1","1");
        when(activeOrderPublisher.publishIsValidEventIDEvent(activeOrder.getEventId())).thenReturn(false);
        boolean result = activeOrderService.saveOrder(activeOrder);
        assertFalse(result);
    }

    @Test
    public void GivenInvalidQuantity_WhenSaveOrder_thenReturnErrorMessage() {
        ActiveOrderItem activeOrder = new ActiveOrderItem("1", "1","1");
        when(activeOrderPublisher.publishIsValidEventIDEvent(activeOrder.getEventId())).thenReturn(true);
        boolean result = activeOrderService.saveOrder(activeOrder);
        assertFalse(result);
    }

    @Test
    public void GivenInvalidOrderId_WhenSaveOrder_thenReturnErrorMessage() {
        ActiveOrderItem activeOrder = new ActiveOrderItem("1", "1","1");
        when(activeOrderPublisher.publishIsValidEventIDEvent(activeOrder.getEventId())).thenReturn(true);
        boolean result = activeOrderService.saveOrder(activeOrder);
        assertFalse(result);
    }

    @Test
    public void GivenValidOrder_WhenCreatePendingOrder_thenGetOrderWithValidDetails(){
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("user");
        String eventId = "1";
        String userId = "user";
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);
        when(activeOrderPublisher.publishIsValidEventIDEvent(eventId)).thenReturn(true);
        when(activeOrderRepoMock.findByUserId(userId)).thenReturn(null); // No existing order
        when(activeOrderRepoMock.save(any())).thenReturn(true);
        ActiveOrderItem order = activeOrderService.createPendingOrder(sessionToken, userId, eventId);
        assertEquals(order.getEventId(), eventId);
        assertEquals(order.getUserId(), userId);
    }

    @Test
    public void GivenValidOrder_WhenCreatePendingOrder_thenOrderIsSavedInRepo(){
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("user");
        String eventId = "1";
        String userId = "user";
        when(activeOrderPublisher.publishIsValidEventIDEvent(eventId)).thenReturn(true);
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);
        when(activeOrderRepoMock.findByUserId(userId)).thenReturn(null); // No existing order
        when(activeOrderRepoMock.save(any())).thenReturn(true);
        ActiveOrderItem order = activeOrderService.createPendingOrder(sessionToken, userId, eventId);
        verify(activeOrderRepoMock).save(order);
    }

    @Test
    public void GivenInvalidSessionToken_WhenCreatePendingOrder_thenReturnErrorMessage(){
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("user");
        String eventId = "1";
        String userId = "user";
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(false);
        assertThrows(RuntimeException.class, () ->activeOrderService.createPendingOrder(sessionToken, userId, eventId));
    }

    @Test
    public void GivenInvalidEventId_WhenCreatePendingOrder_thenReturnErrorMessage(){
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("user");
        String eventId = "-1";
        String userId = "user";
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);
        when(activeOrderPublisher.publishIsValidEventIDEvent(eventId)).thenReturn(false);
        when(activeOrderRepoMock.findByUserId(userId)).thenReturn(null);
        assertThrows(Exception.class, () ->activeOrderService.createPendingOrder(sessionToken, userId, eventId));
    }
   
    @Test
    public void GivenRepoThrowsException_WhenCreatePendingOrder_thenReturnErrorMessage(){
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("user");
        String eventId = "1";
        String userId = "user";
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);
        when(activeOrderRepoMock.findByUserId(userId)).thenReturn(null);
        when(activeOrderRepoMock.save(any(ActiveOrderItem.class))).thenThrow(new RuntimeException("Error saving order"));
        assertThrows(Exception.class, () ->activeOrderService.createPendingOrder(sessionToken, userId, eventId));
    }

    @Test
    public void givenValidOrder_whenCompleteOrder_thenOrderIsRemovedFromRepo() {
        double amount = 100.0;
        String orderId = "order1";
        SessionToken sessionToken = mock(SessionToken.class);
        ActiveOrderItem order = mock(ActiveOrderItem.class);
        IPaymentGateway paymentGateway = mock(IPaymentGateway.class);
        when(order.getCreatedAt()).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);
        when(order.getOrderId()).thenReturn(orderId);
        when(activeOrderRepoMock.findById(order.getOrderId())).thenReturn(order);
        when(activeOrderRepoMock.markAsProcessing(orderId)).thenReturn(true);
        when(paymentGateway.pay()).thenReturn(true);
        when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(List.of(new BarcodeDTO("barcode")));
        when(activeOrderPublisher.publishIsUpToPolicy(any())).thenReturn(true);
       
        activeOrderService.completeOrder(paymentGateway, sessionToken, amount, orderId);
        verify(activeOrderRepoMock, times(1)).delete(order.getOrderId());
    }
    
    @Test
    public void givenExpiredSessionToken_whenCompleteOrder_thenReturnErrorMessage() {
        SessionToken sessionToken = mock(SessionToken.class);
        ActiveOrderItem order = mock(ActiveOrderItem.class);
        IPaymentGateway paymentGateway = mock(IPaymentGateway.class);
            
        String orderId = "order1";
        when(order.getOrderId()).thenReturn(orderId);
        when(activeOrderRepoMock.findById(order.getOrderId())).thenReturn(order);
        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(false);
        when(paymentGateway.pay()).thenReturn(true);

        verify(activeOrderRepoMock, times(0)).delete(orderId);
        assertThrows(Exception.class, () ->activeOrderService.completeOrder(paymentGateway, sessionToken, 100, orderId));        
    }

    @Test
    public void givenPaymentFailure_whenCompleteOrder_thenReturnErrorMessage() {
        SessionToken sessionToken = mock(SessionToken.class);
        ActiveOrderItem order = mock(ActiveOrderItem.class);
        IPaymentGateway paymentGateway = mock(IPaymentGateway.class);
        String orderId = "order1";

        when(order.getCreatedAt()).thenReturn(new Timestamp(System.currentTimeMillis())); // Order not expired
        when(order.getOrderId()).thenReturn(orderId);
        when(activeOrderRepoMock.findById(order.getOrderId())).thenReturn(order);
        when(activeOrderRepoMock.markAsProcessing(orderId)).thenReturn(true);
        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);
        when(activeOrderPublisher.publishIsUpToPolicy(any())).thenReturn(true);
        when(paymentGateway.pay()).thenReturn(false); // Payment fails

        assertThrows(Exception.class, () ->
            activeOrderService.completeOrder(paymentGateway, sessionToken, 100, orderId)
        );
        // Ensure order is deleted after failure
        verify(activeOrderRepoMock, times(1)).delete(orderId);
    }

    @Test
    public void givenNonExistingOrder_whenCompleteOrder_thenReturnErrorMessage() {
        SessionToken sessionToken = mock(SessionToken.class);
        IPaymentGateway paymentGateway = mock(IPaymentGateway.class);
        String orderId = "nonExistingOrder";
        when(activeOrderRepoMock.findById(orderId)).thenReturn(null);
        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);
        when(paymentGateway.pay()).thenReturn(true);
        
        verify(activeOrderRepoMock, times(0)).delete(orderId);

        assertThrows(Exception.class, () ->activeOrderService.completeOrder(paymentGateway, sessionToken, 100, orderId));

    } 

    @Test
    public void givenValidOrder_whenUpdateOrder_thenReturnUpdatedOrder() {
        ActiveOrderItem order = mock(ActiveOrderItem.class);
        SessionToken sessionToken = mock(SessionToken.class);
        ActiveOrderDTO orderDTO = mock(ActiveOrderDTO.class);
        String orderID = "order1";
        when(activeOrderPublisher.publishReserveSeats(any(), any())).thenReturn(true);
        when(activeOrderPublisher.publishReserveStandingArea(any(), any(), anyInt())).thenReturn(true);

        when(order.getOrderId()).thenReturn(orderID);
        when(orderDTO.getOrderId()).thenReturn(orderID);
        when(activeOrderRepoMock.findById(orderDTO.getOrderId())).thenReturn(order);
        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);
        when(order.getCreatedAt()).thenReturn(new Timestamp(System.currentTimeMillis()));

        //creating the old order
        int oldQuantity = 5;
        HashMap<String, Integer> areaQuantities = new HashMap<>();
        areaQuantities.put("area1", oldQuantity);
        areaQuantities.put("area2", oldQuantity);
        when(order.getStandingAreaQuantities()).thenReturn(areaQuantities);
        List<String> seatIds = List.of("seat1", "seat2");
        when(order.getSeatIds()).thenReturn(seatIds);

        //creating the new order
        int newQuantity = 3;
        List<String> newSeatIds = List.of("seat3", "seat4");
        when(orderDTO.getSeatIds()).thenReturn(newSeatIds);
        HashMap<String, Integer> newAreaQuantities = new HashMap<>();
        newAreaQuantities.put("area1", newQuantity);
        newAreaQuantities.put("area2", newQuantity);
        when(orderDTO.getStandingAreaQuantities()).thenReturn(newAreaQuantities);

        activeOrderService.updateActiveOrder(sessionToken, orderDTO);

        verify(activeOrderPublisher, times(1)).publishReleaseSeats(order.getEventId(), order.getSeatIds());
        verify(activeOrderPublisher, times(1)).publishReserveSeats(order.getEventId(), newSeatIds);

        verify(activeOrderRepoMock, times(1)).update(order);
    }

    @Test
    public void givenNonExistentOrder_whenUpdateOrder_thenThrowException(){
        SessionToken sessionToken = mock(SessionToken.class);
        ActiveOrderDTO orderDTO = mock(ActiveOrderDTO.class);
        String orderId = "nonExistingOrder";
        
        when(orderDTO.getOrderId()).thenReturn(orderId);
        when(activeOrderRepoMock.findById(orderId)).thenReturn(null);

        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);

        assertThrows(Exception.class, ()-> activeOrderService.updateActiveOrder(sessionToken, orderDTO));
    }

    @Test
    public void givenExpiredSessionToken_whenUpdateOrder_thenThrowException(){
        SessionToken sessionToken = mock(SessionToken.class);
        ActiveOrderItem order = mock(ActiveOrderItem.class);
        String orderId = "order1";
        ActiveOrderDTO orderDTO = mock(ActiveOrderDTO.class);
        
        when(orderDTO.getOrderId()).thenReturn(orderId);
        when(activeOrderRepoMock.findById(order.getOrderId())).thenReturn(order);
        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(false);

        assertThrows(Exception.class, ()-> activeOrderService.updateActiveOrder(sessionToken, orderDTO));
    }
    
    @Test
    public void givenExpiredActiveOrder_whenUpdateOrder_thenThrowException(){
        SessionToken sessionToken = mock(SessionToken.class);
        ActiveOrderItem order = mock(ActiveOrderItem.class);
        ActiveOrderDTO orderDTO = mock(ActiveOrderDTO.class);

        when(activeOrderRepoMock.findById(orderDTO.getOrderId())).thenReturn(order);
        when(order.getCreatedAt()).thenReturn(new Timestamp(0)); // Expired

        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);

        when(order.getSeatIds()).thenReturn(List.of());
        when(order.getStandingAreaQuantities()).thenReturn(new HashMap<>());
        when(orderDTO.getSeatIds()).thenReturn(List.of());
        when(orderDTO.getStandingAreaQuantities()).thenReturn(new HashMap<>());
        
        assertThrows(Exception.class, ()-> activeOrderService.updateActiveOrder(sessionToken, orderDTO));
        verify(activeOrderRepoMock, times(0)).update(order);
    }

    @Test
    public void givenValidOrderButCantReserve_whenUpdateOrder_thenThrowException(){
        SessionToken sessionToken = mock(SessionToken.class);
        ActiveOrderItem order = mock(ActiveOrderItem.class);
        String orderId = "order1";
        int quantity = 5;
        String eventId = "event1";

        when(order.getCreatedAt()).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(order.getOrderId()).thenReturn(orderId);
        when(order.getEventId()).thenReturn(eventId);
        when(order.getSeatIds()).thenReturn(List.of("seat1", "seat2"));
        HashMap<String, Integer> seatQuantities = new HashMap<>();
        seatQuantities.put("seat1", quantity);
        seatQuantities.put("seat2", quantity);
        when(order.getStandingAreaQuantities()).thenReturn(seatQuantities);
        
        ActiveOrderDTO orderDTO = mock(ActiveOrderDTO.class);
        when(orderDTO.getOrderId()).thenReturn(orderId);
        when(orderDTO.getSeatIds()).thenReturn(List.of("seat3", "seat4"));
        
        // Simulate failure to reserve new seats
        when(activeOrderPublisher.publishReserveSeats(eventId, List.of("seat3", "seat4"))).thenReturn(false);

        when(activeOrderRepoMock.findById(order.getOrderId())).thenReturn(order);
        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true); 
        
        assertThrows(Exception.class, ()-> activeOrderService.updateActiveOrder(sessionToken, orderDTO));
    }
    @Test
    public void givenExistingOrder_whenCreatePendingOrder_thenThrowException() {
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("userToken");
        when(authenticationService.validate("userToken")).thenReturn(true);
        
        // Simulating that the user already has an active order
        ActiveOrderItem existingOrder = new ActiveOrderItem("oldOrder", "user123", "event1");
        when(activeOrderRepoMock.findByUserId("user123")).thenReturn(existingOrder);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            activeOrderService.createPendingOrder(sessionToken, "user123", "event1");
        });
        
        assertEquals("an active order already exists for this user: user123", exception.getMessage());
        verify(activeOrderRepoMock, never()).save(any());
    }

    @Test
    public void givenValidOrder_whenAddSeatsToActiveOrder_thenSeatsAreReserved() {
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("userToken");
        when(authenticationService.validate("userToken")).thenReturn(true);

        ActiveOrderItem order = new ActiveOrderItem("order1", "user1", "event1");
        order.setCreatedAt(new Timestamp(System.currentTimeMillis())); // not expired
        when(activeOrderRepoMock.findById("order1")).thenReturn(order);
        
        List<String> seats = List.of("A-10", "A-11");

        when(activeOrderPublisher.publishIsValidEventIDEvent("event1")).thenReturn(true);
        when(activeOrderPublisher.publishReserveSeats("event1", seats)).thenReturn(true);
        when(activeOrderRepoMock.save(order)).thenReturn(true);

        activeOrderService.addSeatsToActiveOrder(sessionToken, "order1", seats);

        assertTrue(order.getSeatIds().containsAll(seats));
        verify(activeOrderPublisher, times(1)).publishReserveSeats("event1", seats);
        verify(activeOrderRepoMock, times(1)).save(order);
    }

    @Test
    public void givenInsufficientQuantity_whenAddStandingArea_thenThrowException() {
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("userToken");
        when(authenticationService.validate("userToken")).thenReturn(true);

        ActiveOrderItem order = new ActiveOrderItem("order1", "user1", "event1");
        order.setCreatedAt(new Timestamp(System.currentTimeMillis())); // not expired
        when(activeOrderRepoMock.findById("order1")).thenReturn(order);
        
        // publishReserveStandingArea returns false (meaning insufficient quantity)
        when(activeOrderPublisher.publishReserveStandingArea("event1", "GA-1", 3)).thenReturn(false);

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            activeOrderService.addStandingAreaToActiveOrder(sessionToken, "order1", "GA-1", 3);
        });
        
        assertEquals("cant reserve these standing area tickets", exception.getMessage());
        verify(activeOrderRepoMock, never()).save(any());
    }
    @Test
    public void givenExpiredOrder_whenCompleteOrder_thenThrowExceptionAndRollback() {
        // Arrange
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("userToken");
        when(authenticationService.validate("userToken")).thenReturn(true);

        ActiveOrderItem order = new ActiveOrderItem("order1", "user1", "event1");
        order.setCreatedAt(new Timestamp(System.currentTimeMillis() - (30 * 60 * 1000)));

        order.setSeatIds(List.of("A-1", "A-2"));
        HashMap<String, Integer> standingArea = new HashMap<>();
        standingArea.put("GA-1", 3);
        order.setStandingAreaQuantities(standingArea);

        when(activeOrderRepoMock.findById("order1")).thenReturn(order);
        IPaymentGateway paymentGateway = mock(IPaymentGateway.class);
        when(activeOrderRepoMock.markAsProcessing("order1")).thenReturn(true);
        // Act & +
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            activeOrderService.completeOrder(paymentGateway, sessionToken, 100.0, "order1");
        });

        assertEquals("Order has expired", exception.getMessage());

        // Verify Rollback
        verify(activeOrderPublisher, times(1)).publishReleaseSeats("event1", List.of("A-1", "A-2"));
        verify(activeOrderPublisher, times(1)).publishReleaseStandingArea("event1", "GA-1", 3);
        verify(activeOrderRepoMock, times(1)).delete("order1"); // ההזמנה חייבת להימחק
        verify(paymentGateway, never()).pay(); // מוודא שבכלל לא ניסינו לחייב
    }
    @Test
    public void givenPaymentFails_whenCompleteOrder_thenThrowExceptionAndRollback() {
        // Arrange
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("userToken");
        when(authenticationService.validate("userToken")).thenReturn(true);

        ActiveOrderItem order = new ActiveOrderItem("order1", "user1", "event1");
        order.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        order.setSeatIds(List.of("B-10", "B-11"));
        HashMap<String, Integer> standingArea = new HashMap<>();
        standingArea.put("VIP-1", 2);
        order.setStandingAreaQuantities(standingArea);

        when(activeOrderRepoMock.findById("order1")).thenReturn(order);
        when(activeOrderPublisher.publishIsUpToPolicy(any())).thenReturn(true);

        IPaymentGateway paymentGateway = mock(IPaymentGateway.class);
        when(paymentGateway.pay()).thenReturn(false);
        when(activeOrderRepoMock.markAsProcessing("order1")).thenReturn(true);
        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            activeOrderService.completeOrder(paymentGateway, sessionToken, 250.0, "order1");
        });

        assertEquals("Payment failed", exception.getMessage());

        // Verify Rollback
        verify(activeOrderPublisher, times(1)).publishReleaseSeats("event1", List.of("B-10", "B-11"));
        verify(activeOrderPublisher, times(1)).publishReleaseStandingArea("event1", "VIP-1", 2);
        verify(activeOrderRepoMock, times(1)).delete("order1");
    }
    @Test
    public void givenBarcodeGenerationFails_whenCompleteOrder_thenRollback() {
        // Arrange
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("userToken");
        when(authenticationService.validate("userToken")).thenReturn(true);

        ActiveOrderItem order = new ActiveOrderItem("order1", "user1", "event1");
        order.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        order.setSeatIds(List.of("C-1"));
        HashMap<String, Integer> standingArea = new HashMap<>();
        standingArea.put("GA-2", 1);
        order.setStandingAreaQuantities(standingArea);

        when(activeOrderRepoMock.markAsProcessing("order1")).thenReturn(true);
        when(activeOrderRepoMock.findById("order1")).thenReturn(order);
        when(activeOrderPublisher.publishIsUpToPolicy(any())).thenReturn(true);

        IPaymentGateway paymentGateway = mock(IPaymentGateway.class);
        when(paymentGateway.pay()).thenReturn(true);
        when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            activeOrderService.completeOrder(paymentGateway, sessionToken, 150.0, "order1");
        });

        assertEquals("Barcode generation failed. Refund processed.", exception.getMessage());

        // Verify Rollback
        verify(activeOrderPublisher, times(1)).publishReleaseSeats("event1", List.of("C-1"));
        verify(activeOrderPublisher, times(1)).publishReleaseStandingArea("event1", "GA-2", 1);
        verify(activeOrderRepoMock, times(1)).delete("order1");
    }
    @Test
    public void givenBarcodeIssuanceFails_WhenCompleteOrder_thenDontCharge(){
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("userToken");
        when(authenticationService.validate("userToken")).thenReturn(true);

        ActiveOrderItem order = new ActiveOrderItem("order1", "user1", "event1");
        order.setCreatedAt(new Timestamp(System.currentTimeMillis())); // not expired
        order.setSeatIds(List.of("A-1", "A-2")); // Mocking already chosen seats
        when(activeOrderRepoMock.findById("order1")).thenReturn(order);
        when(activeOrderRepoMock.markAsProcessing("order1")).thenReturn(true);
        when(activeOrderPublisher.publishIsUpToPolicy(any())).thenReturn(true);

        IPaymentGateway paymentGateway = mock(IPaymentGateway.class);
        when(paymentGateway.pay()).thenReturn(true); // Payment succeeds
        when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> {
            activeOrderService.completeOrder(paymentGateway, sessionToken, 100.0, "order1");
        });
        verify(paymentGateway, times(0)).pay();
    }
    @Test
    public void givenBarcodeIssuanceFails_whenCompleteOrder_thenRollback() {
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("userToken");
        when(authenticationService.validate("userToken")).thenReturn(true);

        ActiveOrderItem order = new ActiveOrderItem("order1", "user1", "event1");
        order.setCreatedAt(new Timestamp(System.currentTimeMillis())); // not expired
        order.setSeatIds(List.of("A-1", "A-2")); // Mocking already chosen seats
        when(activeOrderRepoMock.findById("order1")).thenReturn(order);
        when(activeOrderRepoMock.markAsProcessing("order1")).thenReturn(true);
        when(activeOrderPublisher.publishIsUpToPolicy(any())).thenReturn(true);
        
        IPaymentGateway paymentGateway = mock(IPaymentGateway.class);
        when(paymentGateway.pay()).thenReturn(true); // Payment succeeds
        
        // Barcode generation fails (returns null)
        when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(null);

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            activeOrderService.completeOrder(paymentGateway, sessionToken, 100.0, "order1");
        });

        assertEquals("Barcode generation failed. Refund processed.", exception.getMessage());

        // Verify rollback logic (seats are released)
        verify(activeOrderPublisher, times(1)).publishReleaseSeats("event1", List.of("A-1", "A-2"));
        
        // Verify order is deleted
        verify(activeOrderRepoMock, times(1)).delete("order1");
    }
    @Test
    public void GivenTwoDifferentUsers_WhenCreatePendingOrderConcurrently_ThenBothSucceedWithUniqueOrderIds() throws InterruptedException {
        // use real repo so concurrency is actually tested
        ActiveOrderMemRepo realRepo = new ActiveOrderMemRepo();
        ActiveOrderService service = new ActiveOrderService(new ActiveOrderListener(realRepo), activeOrderPublisher, realRepo, activeOrderHandler,
                authenticationService, barcodeGatewayMock);

        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("token");
        when(authenticationService.validate("token")).thenReturn(true);
        when(activeOrderPublisher.publishIsValidEventIDEvent(any())).thenReturn(true);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        List<ActiveOrderItem> results = Collections.synchronizedList(new ArrayList<>());
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        new Thread(() -> {
            try {
                startLatch.await();
                results.add(service.createPendingOrder(sessionToken, "userA", "event1"));
            } catch (Exception e) { errors.add(e); }
            finally { doneLatch.countDown(); }
        }).start();

        new Thread(() -> {
            try {
                startLatch.await();
                results.add(service.createPendingOrder(sessionToken, "userB", "event1"));
            } catch (Exception e) { errors.add(e); }
            finally { doneLatch.countDown(); }
        }).start();

        startLatch.countDown();
        doneLatch.await();

        assertEquals(2, results.size(), "Both users should successfully create an order");
        assertTrue(errors.isEmpty(), "No exceptions should be thrown");
        assertNotEquals(results.get(0).getOrderId(), results.get(1).getOrderId(),
                "Each order must have a unique order ID");
    }

    @Test
    public void GivenSameUser_WhenCreatePendingOrderTwiceConcurrently_ThenOnlyOneSucceeds() throws InterruptedException {
        com.ticketpurchasingsystem.project.infrastructure.ActiveOrderMemRepo realRepo =
                new com.ticketpurchasingsystem.project.infrastructure.ActiveOrderMemRepo();
        ActiveOrderService service = new ActiveOrderService(
                new ActiveOrderListener(realRepo), activeOrderPublisher, realRepo, activeOrderHandler,
                authenticationService, barcodeGatewayMock);

        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("token");
        when(authenticationService.validate("token")).thenReturn(true);
        when(activeOrderPublisher.publishIsValidEventIDEvent(any())).thenReturn(true);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    service.createPendingOrder(sessionToken, "sameUser", "event1");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally { doneLatch.countDown(); }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        assertEquals(1, successCount.get(), "Only one thread should successfully create an order");
        assertEquals(1, failCount.get(), "The other thread should get an IllegalArgumentException");
        assertNotNull(realRepo.findByUserId("sameUser"), "Exactly one order should exist for the user");
    }

    @Test
    public void GivenNDifferentUsers_WhenCreatePendingOrderConcurrently_ThenAllSucceedWithUniqueOrderIds() throws InterruptedException {
        int N = 20;
        com.ticketpurchasingsystem.project.infrastructure.ActiveOrderMemRepo realRepo =
                new com.ticketpurchasingsystem.project.infrastructure.ActiveOrderMemRepo();
        ActiveOrderService service = new ActiveOrderService(
                new ActiveOrderListener(realRepo), activeOrderPublisher, realRepo, activeOrderHandler,
                authenticationService, barcodeGatewayMock);

        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("token");
        when(authenticationService.validate("token")).thenReturn(true);
        when(activeOrderPublisher.publishIsValidEventIDEvent(any())).thenReturn(true);

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(N);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(N);
        List<ActiveOrderItem> results = Collections.synchronizedList(new ArrayList<>());
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < N; i++) {
            final String userId = "user" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    results.add(service.createPendingOrder(sessionToken, userId, "event1"));
                } catch (Exception e) { errors.add(e); }
                finally { doneLatch.countDown(); }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(N, results.size(), "All " + N + " users should successfully create an order");
        assertTrue(errors.isEmpty(), "No exceptions should be thrown");
        long uniqueOrderIds = results.stream().map(ActiveOrderItem::getOrderId).distinct().count();
        assertEquals(N, uniqueOrderIds, "Every order must have a unique order ID");
    }

    // =========================================================================
    // Concurrency Tests - completeOrder
    // =========================================================================

    @Test
    public void GivenSameOrder_WhenCompleteOrderTwiceConcurrently_ThenOnlyOneSucceedsAndUserChargedOnce() throws InterruptedException {
        com.ticketpurchasingsystem.project.infrastructure.ActiveOrderMemRepo realRepo =
                new com.ticketpurchasingsystem.project.infrastructure.ActiveOrderMemRepo();
        ActiveOrderService service = new ActiveOrderService(
                new ActiveOrderListener(realRepo), activeOrderPublisher, realRepo, activeOrderHandler,
                authenticationService, barcodeGatewayMock);

        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("token");
        when(authenticationService.validate("token")).thenReturn(true);
        when(activeOrderPublisher.publishIsValidEventIDEvent(any())).thenReturn(true);
        when(activeOrderPublisher.publishIsUpToPolicy(any())).thenReturn(true);

        IPaymentGateway paymentGateway = mock(IPaymentGateway.class);
        when(paymentGateway.pay()).thenReturn(true);
        when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(List.of(mock(BarcodeDTO.class)));

        // create the order upfront before the concurrent phase
        ActiveOrderItem order = service.createPendingOrder(sessionToken, "userA", "event1");
        String orderId = order.getOrderId();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    service.completeOrder(paymentGateway, sessionToken, 100.0, orderId);
                    successCount.incrementAndGet();
                } catch (IllegalArgumentException e) {
                    // expected — second thread should not find the order
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally { doneLatch.countDown(); }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        assertEquals(1, successCount.get(), "Only one thread should successfully complete the order");
        assertEquals(1, failCount.get(), "The second thread should fail — order already completed");
        verify(paymentGateway, times(1)).pay(); // user charged exactly once
        assertNull(realRepo.findById(orderId), "Order should be deleted from repo after completion");
    }

    @Test
    public void GivenSameOrder_WhenCompleteAndCancelConcurrently_ThenOnlyOneSucceedsAndOrderRemoved() throws InterruptedException {
        com.ticketpurchasingsystem.project.infrastructure.ActiveOrderMemRepo realRepo =
                new com.ticketpurchasingsystem.project.infrastructure.ActiveOrderMemRepo();
        ActiveOrderService service = new ActiveOrderService(
                new ActiveOrderListener(realRepo), activeOrderPublisher, realRepo, activeOrderHandler,
                authenticationService, barcodeGatewayMock);

        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("token");
        when(authenticationService.validate("token")).thenReturn(true);
        when(activeOrderPublisher.publishIsValidEventIDEvent(any())).thenReturn(true);
        when(activeOrderPublisher.publishIsUpToPolicy(any())).thenReturn(true);

        IPaymentGateway paymentGateway = mock(IPaymentGateway.class);
        when(paymentGateway.pay()).thenReturn(true);
        when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(List.of(mock(BarcodeDTO.class)));

        ActiveOrderItem order = service.createPendingOrder(sessionToken, "userB", "event1");
        String orderId = order.getOrderId();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failCount = new java.util.concurrent.atomic.AtomicInteger(0);

        // Thread 1 — tries to complete the order
        new Thread(() -> {
            try {
                startLatch.await();
                service.completeOrder(paymentGateway, sessionToken, 100.0, orderId);
                successCount.incrementAndGet();
            } catch (Exception e) { failCount.incrementAndGet(); }
            finally { doneLatch.countDown(); }
        }).start();

        // Thread 2 — tries to cancel the order
        new Thread(() -> {
            try {
                startLatch.await();
                service.cancelActiveOrder(sessionToken, "userB", orderId);
                successCount.incrementAndGet();
            } catch (Exception e) { failCount.incrementAndGet(); }
            finally { doneLatch.countDown(); }
        }).start();

        startLatch.countDown();
        doneLatch.await();

        assertEquals(1, successCount.get(), "Only one operation should succeed");
        assertEquals(1, failCount.get(), "The other operation should fail gracefully");
        assertNull(realRepo.findById(orderId), "Order should be removed from repo either way");
        verify(paymentGateway, atMostOnce()).pay();
    }

    @Test
    public void GivenNDifferentOrders_WhenCompleteOrderConcurrently_ThenAllSucceedAndAllOrdersRemoved() throws InterruptedException {
        int N = 20;
        com.ticketpurchasingsystem.project.infrastructure.ActiveOrderMemRepo realRepo =
                new com.ticketpurchasingsystem.project.infrastructure.ActiveOrderMemRepo();
        ActiveOrderService service = new ActiveOrderService(
                new ActiveOrderListener(realRepo), activeOrderPublisher, realRepo, activeOrderHandler,
                authenticationService, barcodeGatewayMock);

        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("token");
        when(authenticationService.validate("token")).thenReturn(true);
        when(activeOrderPublisher.publishIsValidEventIDEvent(any())).thenReturn(true);
        when(activeOrderPublisher.publishIsUpToPolicy(any())).thenReturn(true);

        IPaymentGateway paymentGateway = mock(IPaymentGateway.class);
        when(paymentGateway.pay()).thenReturn(true);
        when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(List.of(mock(BarcodeDTO.class)));

        // create N orders upfront before the concurrent phase
        List<String> orderIds = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < N; i++) {
            ActiveOrderItem o = service.createPendingOrder(sessionToken, "user" + i, "event1");
            orderIds.add(o.getOrderId());
        }

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(N);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(N);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        for (String orderId : orderIds) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    service.completeOrder(paymentGateway, sessionToken, 100.0, orderId);
                    successCount.incrementAndGet();
                } catch (Exception e) { errors.add(e); }
                finally { doneLatch.countDown(); }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(N, successCount.get(), "All " + N + " orders should complete successfully");
        assertTrue(errors.isEmpty(), "No exceptions should be thrown: " + errors);
        for (String orderId : orderIds) {
            assertNull(realRepo.findById(orderId), "Order " + orderId + " should be deleted after completion");
        }
        verify(paymentGateway, times(N)).pay(); // each order charged exactly once
    }

    @Test
    public void GivenValidUserAndOwnOrder_WhenHandlerGetActiveOrderInfo_ThenReturnCompleteDTO() {
        ActiveOrderItem order = orderWithSeats(USER_ID);
        order.addStandingAreaQuantity("area-A", 3);

        ActiveOrderDTO result = activeOrderHandler.getActiveOrderInfo(USER_ID, order);

        assertNotNull(result);
        assertEquals(ORDER_ID, result.getOrderId());
        assertEquals(USER_ID, result.getUserId());
        assertEquals(EVENT_ID, result.getEventId());
        assertEquals(order.getCreatedAt(), result.getCreatedAt());
        assertEquals(List.of("seat-1", "seat-2"), result.getSeatIds());
        assertEquals(3, result.getStandingAreaQuantities().get("area-A"));
    }
    @Test
    public void GivenInvalidSession_WhenGetActiveOrderInfo_ThenThrowIllegalArgumentException() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);
        assertThrows(Exception.class, () -> activeOrderService.getActiveOrderInfo(INVALID_SESSION, ORDER_ID));
    }

    @Test
    public void GivenOrderNotFound_WhenGetActiveOrderInfo_ThenThrowIllegalArgumentException() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(null);

        assertThrows(Exception.class,
                () -> activeOrderService.getActiveOrderInfo(VALID_SESSION, ORDER_ID));

    }

    @Test
    public void GivenOrderBelongsToOtherUser_WhenGetActiveOrderInfo_ThenThrowIllegalArgumentException() {
        ActiveOrderItem order = orderForUser(OTHER_USER_ID); // order belongs to OTHER_USER_ID
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID); // but caller is USER_ID
        when(activeOrderRepoMock.findById(ORDER_ID)).thenReturn(order);

        assertThrows(Exception.class,
                () -> activeOrderService.getActiveOrderInfo(VALID_SESSION, ORDER_ID));
    }

    @Test
    public void GivenNullOrderId_WhenGetActiveOrderInfo_ThenThrowIllegalArgumentException() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(activeOrderRepoMock.findById(null)).thenReturn(null);

        assertThrows(Exception.class,
                () -> activeOrderService.getActiveOrderInfo(VALID_SESSION, null));
    }
    @Test
    public void GivenNullUserId_WhenHandlerGetActiveOrderInfo_ThenReturnNull() {
        ActiveOrderItem order = orderForUser(USER_ID);

        ActiveOrderDTO result = activeOrderHandler.getActiveOrderInfo(null, order);

        assertNull(result);
    }

    @Test
    public void GivenNullOrder_WhenHandlerGetActiveOrderInfo_ThenReturnNull() {
        ActiveOrderDTO result = activeOrderHandler.getActiveOrderInfo(USER_ID, null);

        assertNull(result);
    }

    @Test
    public void GivenOrderWithNullOrderId_WhenHandlerGetActiveOrderInfo_ThenReturnNull() {
        ActiveOrderItem order = orderForUser(USER_ID);
        order.setOrderId(null);

        ActiveOrderDTO result = activeOrderHandler.getActiveOrderInfo(USER_ID, order);

        assertNull(result);
    }
}