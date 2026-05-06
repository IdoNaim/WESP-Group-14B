package com.ticketpurchasingsystem.project.domain.ActiveOrders;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.ticketpurchasingsystem.project.application.IPaymentGateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ticketpurchasingsystem.project.application.ActiveOrderService;
import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.IActiveOrderService;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

import com.ticketpurchasingsystem.project.application.IBarCodeGateway;

public class ActiveOrderTests {
    private IActiveOrderRepo activeOrderRepoMock;
    private IActiveOrderService activeOrderService;
    //private ActiveOrderListener activeOrderListener;
    private ActiveOrderPublisher activeOrderPublisher;
    private AuthenticationService authenticationService;
    private IBarCodeGateway barcodeGatewayMock;
    @BeforeEach
    public void setUp() {
        activeOrderRepoMock = mock(IActiveOrderRepo.class);
        activeOrderPublisher = mock(ActiveOrderPublisher.class);
        authenticationService = mock(AuthenticationService.class);
        barcodeGatewayMock = mock(IBarCodeGateway.class);
        activeOrderService = new ActiveOrderService(new ActiveOrderListener(activeOrderRepoMock), activeOrderPublisher, activeOrderRepoMock,authenticationService, barcodeGatewayMock);

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
        when(activeOrderPublisher.publishIsValidEventIDEvent("1")).thenReturn(true);
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);
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
    //TODO:
    @Test
    public void GivenInvalidEventId_WhenCreatePendingOrder_thenReturnErrorMessage(){
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("user");
        String eventId = "-1";
        String userId = "user";
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);
        assertThrows(Exception.class, () ->activeOrderService.createPendingOrder(sessionToken, userId, eventId));
    }
   
    @Test
    public void GivenRepoThrowsException_WhenCreatePendingOrder_thenReturnErrorMessage(){
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("user");
        String eventId = "1";
        String userId = "user";
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);
        when(activeOrderRepoMock.save(any(ActiveOrderItem.class))).thenThrow(new RuntimeException("Error saving order"));
        assertThrows(Exception.class, () ->activeOrderService.createPendingOrder(sessionToken, userId, eventId));
    }

    @Test
    public void givenValidOrder_whenCompleteOrder_thenOrderIsRemovedFromRepo() {
        double amount = 100.0;
        SessionToken sessionToken = mock(SessionToken.class);
        ActiveOrderItem order = mock(ActiveOrderItem.class);
        IPaymentGateway paymentGateway = mock(IPaymentGateway.class);

        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);
        when(order.getOrderId()).thenReturn("order1");
        when(activeOrderRepoMock.findById(order.getOrderId())).thenReturn(order);
        when(activeOrderService.payment(eq(paymentGateway), any(), anyDouble())).thenReturn(true);
        when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(List.of(new BarcodeDTO("barcode")));
        when(activeOrderPublisher.publishIsUpToPolicy(any())).thenReturn(true);
       
        activeOrderService.completeOrder(paymentGateway, sessionToken, amount, order.getOrderId());
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
        when(activeOrderService.payment(eq(paymentGateway), any(), any())).thenReturn(true);

        verify(activeOrderRepoMock, times(0)).delete(orderId);
        assertThrows(Exception.class, () ->activeOrderService.completeOrder(paymentGateway, sessionToken, 100, orderId));        
    }

    @Test
    public void givenPaymentFailure_whenCompleteOrder_thenReturnErrorMessage() {
        SessionToken sessionToken = mock(SessionToken.class);
        ActiveOrderItem order = mock(ActiveOrderItem.class);
        IPaymentGateway paymentGateway = mock(IPaymentGateway.class);
        String orderId = "order1";
        when(order.getOrderId()).thenReturn(orderId);
        when(activeOrderRepoMock.findById(order.getOrderId())).thenReturn(order);
        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);
        when(activeOrderService.payment(eq(paymentGateway), any(), anyDouble())).thenReturn(false);
        verify(activeOrderRepoMock, times(0)).delete(orderId);

        assertThrows(Exception.class, () ->activeOrderService.completeOrder(paymentGateway, sessionToken, 100, orderId));        
    }

    @Test
    public void givenNonExistingOrder_whenCompleteOrder_thenReturnErrorMessage() {
        SessionToken sessionToken = mock(SessionToken.class);
        IPaymentGateway paymentGateway = mock(IPaymentGateway.class);
        String orderId = "nonExistingOrder";
        when(activeOrderRepoMock.findById(orderId)).thenReturn(null);
        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);
        when(activeOrderService.payment(eq(paymentGateway), any(), anyDouble())).thenReturn(true);
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

        when(order.getOrderId()).thenReturn(orderID);
        when(orderDTO.getOrderId()).thenReturn(orderID);
        when(activeOrderRepoMock.findById(orderDTO.getOrderId())).thenReturn(order);
        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);

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

        //we need to veify that the old seats were released and the new ones were reserved, and that the order was updated in the repo
        verify(activeOrderPublisher, times(1)).publishReleaseSeats(order.getEventId(), order.getSeatIds());
        verify(activeOrderPublisher, times(1)).publishReserveSeats(order.getEventId(), newSeatIds);

//        for(String areaId : areaQuantities.keySet()) {
//            verify(activeOrderPublisher, times(1)).publishReleaseStandingArea(order.getEventId(), areaId, seatQuantities.get(areaId));
//        }
//
//        for(String areaId : newSeatQuantities.keySet()) {
//            verify(activeOrderPublisher, times(1)).publishReserveStandingArea(order.getEventId(), areaId, newSeatQuantities.get(areaId));
//        }
        // standing areas: service releases/reserves the DIFFERENCE
        for(String areaId : newAreaQuantities.keySet()) {
            int current = areaQuantities.getOrDefault(areaId, 0);
            int next = newAreaQuantities.get(areaId);
            if(next > current) {
                verify(activeOrderPublisher, times(1))
                        .publishReserveStandingArea(order.getEventId(), areaId, next - current);
            } else if(next < current) {
                verify(activeOrderPublisher, times(1))
                        .publishReleaseStandingArea(order.getEventId(), areaId, current - next); // 5 - 3 = 2
            }
        }

// areas in old order but not in new order should be fully released
        for(String areaId : areaQuantities.keySet()) {
            if(!newAreaQuantities.containsKey(areaId)) {
                verify(activeOrderPublisher, times(1))
                        .publishReleaseStandingArea(order.getEventId(), areaId, areaQuantities.get(areaId));
            }
        }

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
        // when(order.getOrderId()).thenReturn(orderId);
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
        when(order.getCreatedAt()).thenReturn(new Timestamp(0));

        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);

        when(order.getSeatIds()).thenReturn(List.of());
        when(order.getStandingAreaQuantities()).thenReturn(new HashMap<>());
        when(orderDTO.getSeatIds()).thenReturn(List.of());
        when(orderDTO.getStandingAreaQuantities()).thenReturn(new HashMap<>());
        
        
        assertThrows(Exception.class, ()-> activeOrderService.updateActiveOrder(sessionToken, orderDTO));
        verify(activeOrderService, times(0)).saveOrder(any());


        // String orderId = "order1";
        // String eventId = "event1";
        
        // when(order.getCreatedAt()).thenReturn(new Timestamp(0));

        // when(order.getOrderId()).thenReturn(orderId);

        // when(order.getEventId()).thenReturn(eventId);

        // when(activeOrderPublisher.publishReserveTickets(eventId, quantity)).thenReturn(true);
        // //when(activeOrderPublisher.publishUnreserveTickets(eventId, any())).thenReturn(true);
        // when(activeOrderRepoMock.findById(order.getOrderId())).thenReturn(order);
        // when(sessionToken.getToken()).thenReturn("user");
        // when(authenticationService.validate(sessionToken.getToken())).thenReturn(true);
        // assertThrows(Exception.class, ()-> activeOrderService.updateActiveOrder(sessionToken, o));
    }
    @Test
    public void givenValidOrderButCantReserve_whenUpdateOrder_thenThrowException(){
        SessionToken sessionToken = mock(SessionToken.class);
        ActiveOrderItem order = mock(ActiveOrderItem.class);
        String orderId = "order1";
        int quantity = 5;
        int newQuantity = 10;
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

        when(activeOrderRepoMock.findById(order.getOrderId())).thenReturn(order);
        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validate(sessionToken.getToken())).thenReturn(true); 
        assertThrows(Exception.class, ()-> activeOrderService.updateActiveOrder(sessionToken, orderDTO));
        
        


        // when(activeOrderPublisher.publishReserveTickets(eventId, quantity)).thenReturn(false);
        // //when(activeOrderPublisher.publishUnreserveTickets(eventId, any())).thenReturn(true);
        // when(activeOrderRepoMock.findById(order.getOrderId())).thenReturn(order);
        // when(sessionToken.getToken()).thenReturn("user");
        // when(authenticationService.validateToken(sessionToken.getToken())).thenReturn(true);
        // assertThrows(Exception.class, ()-> activeOrderService.updateActiveOrder(sessionToken, orderId,newQuantity));
    }
}