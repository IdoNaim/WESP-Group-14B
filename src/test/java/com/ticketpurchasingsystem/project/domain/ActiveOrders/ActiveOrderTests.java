package com.ticketpurchasingsystem.project.domain.ActiveOrders;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

public class ActiveOrderTests {
    private IActiveOrderRepo activeOrderRepoMock;
    private IActiveOrderService activeOrderService;
    //private ActiveOrderListener activeOrderListener;
    private ActiveOrderPublisher activeOrderPublisher;
    private AuthenticationService authenticationService;
    @BeforeEach
    public void setUp() {
        activeOrderRepoMock = mock(IActiveOrderRepo.class);
        activeOrderPublisher = mock(ActiveOrderPublisher.class);
        authenticationService = mock(AuthenticationService.class);
        activeOrderService = new ActiveOrderService(new ActiveOrderListener(activeOrderRepoMock), activeOrderPublisher, activeOrderRepoMock,authenticationService);

    }
    @Test
    public void GivenValidOrder_WhenSaveOrder_thenReturnTrue() {
        ActiveOrderItem activeOrder = new ActiveOrderItem("1", "1","1",1);
        when(activeOrderRepoMock.save(activeOrder)).thenReturn(true);
        when(activeOrderPublisher.publishIsValidEventIDEvent(activeOrder.getEventId())).thenReturn(true);
        boolean result = activeOrderService.saveOrder(activeOrder);
        assertTrue(result);
    }
    @Test
    public void GivenValidOrder_WhenSaveOrder_thenCallSaveInRepo() {
        ActiveOrderItem activeOrder = new ActiveOrderItem("1", "1","1",1);
        when(activeOrderRepoMock.save(activeOrder)).thenReturn(true);
        when(activeOrderPublisher.publishIsValidEventIDEvent(activeOrder.getEventId())).thenReturn(true);
        boolean result = activeOrderService.saveOrder(activeOrder);
        verify(activeOrderRepoMock, times(1)).save(activeOrder);
        assertTrue(result);
    }
    @Test
    public void GivenThrownException_WhenSaveOrder_thenReturnFalse() {
        ActiveOrderItem activeOrder = new ActiveOrderItem("1", "1","1",1);
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
        ActiveOrderItem activeOrder = new ActiveOrderItem("1", "1","1",1);
        when(activeOrderPublisher.publishIsValidEventIDEvent(activeOrder.getEventId())).thenReturn(false);
        boolean result = activeOrderService.saveOrder(activeOrder);
        assertFalse(result);
    }

    @Test
    public void GivenInvalidQuantity_WhenSaveOrder_thenReturnErrorMessage() {
        ActiveOrderItem activeOrder = new ActiveOrderItem("1", "1","1",1);
        when(activeOrderPublisher.publishIsValidEventIDEvent(activeOrder.getEventId())).thenReturn(true);
        boolean result = activeOrderService.saveOrder(activeOrder);
        assertFalse(result);
    }

    @Test
    public void GivenInvalidOrderId_WhenSaveOrder_thenReturnErrorMessage() {
        ActiveOrderItem activeOrder = new ActiveOrderItem("1", "1","1",1);
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
        int quantity = 5 ;
        when(authenticationService.validateToken(sessionToken.getToken())).thenReturn(true);
        when(activeOrderPublisher.publishReserveTickets(eventId, quantity)).thenReturn(true);
        ActiveOrderItem order = activeOrderService.createPendingOrder(sessionToken, userId, eventId,quantity);
        assertEquals(order.getQuantity(), quantity);
        assertEquals(order.getEventId(), eventId);
        assertEquals(order.getUserId(), userId);
    }
    @Test
    public void GivenValidOrder_WhenCreatePendingOrder_thenOrderIsSavedInRepo(){
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("user");
        String eventId = "1";
        String userId = "user";
        int quantity = 5 ;
        when(authenticationService.validateToken(sessionToken.getToken())).thenReturn(true);
        when(activeOrderPublisher.publishReserveTickets(eventId, quantity)).thenReturn(true);
        ActiveOrderItem order = activeOrderService.createPendingOrder(sessionToken, userId, eventId,quantity);
        verify(activeOrderService.saveOrder(order));
    }
    @Test
    public void GivenInvalidSessionToken_WhenCreatePendingOrder_thenReturnErrorMessage(){
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("user");
        String eventId = "1";
        String userId = "user";
        int quantity = 5 ;
        when(authenticationService.validateToken(sessionToken.getToken())).thenReturn(false);
        when(activeOrderPublisher.publishReserveTickets(eventId, quantity)).thenReturn(true);
        assertThrows(RuntimeException.class, () ->activeOrderService.createPendingOrder(sessionToken, userId, eventId, quantity));
    }
    @Test
    public void GivenInvalidEventId_WhenCreatePendingOrder_thenReturnErrorMessage(){
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("user");
        String eventId = "-1";
        String userId = "user";
        int quantity = 5 ;
        when(authenticationService.validateToken(sessionToken.getToken())).thenReturn(true);
        when(activeOrderPublisher.publishReserveTickets(eventId, quantity)).thenReturn(false);
        assertThrows(Exception.class, () ->activeOrderService.createPendingOrder(sessionToken, userId, eventId, quantity));
    }
    @Test
    public void GivenInvalidQuantity_WhenCreatePendingOrder_thenReturnErrorMessage(){
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("user");
        String eventId = "1";
        String userId = "user";
        int quantity = -4 ;
        when(authenticationService.validateToken(sessionToken.getToken())).thenReturn(false);
        when(activeOrderPublisher.publishReserveTickets(eventId, quantity)).thenReturn(true);
        assertThrows(Exception.class, () ->activeOrderService.createPendingOrder(sessionToken, userId, eventId, quantity));
    }
    @Test
    public void GivenRepoThrowsException_WhenCreatePendingOrder_thenReturnErrorMessage(){
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("user");
        String eventId = "1";
        String userId = "user";
        int quantity = 5 ;
        when(authenticationService.validateToken(sessionToken.getToken())).thenReturn(true);
        when(activeOrderPublisher.publishReserveTickets(eventId, quantity)).thenReturn(true);
        when(activeOrderRepoMock.save(any(ActiveOrderItem.class))).thenThrow(new RuntimeException("Error saving order"));
        assertThrows(Exception.class, () ->activeOrderService.createPendingOrder(sessionToken, userId, eventId, quantity));
    }

    @Test
    public void givenValidOrder_whenCompleteOrder_thenOrderIsRemovedFromRepo() {
        SessionToken sessionToken = mock(SessionToken.class);
        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validateToken(sessionToken.getToken())).thenReturn(true);
        ActiveOrderItem order = mock(ActiveOrderItem.class);
        when(order.getOrderId()).thenReturn("order1");
        when(activeOrderRepoMock.findById(order.getOrderId())).thenReturn(order);
        IPaymentGateway paymentGateway = mock(IPaymentGateway.class);
        when(activeOrderService.payment(paymentGateway, any(), any())).thenReturn(true);
        activeOrderService.completeOrder(paymentGateway, sessionToken, 100, order.getOrderId());
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
        when(authenticationService.validateToken(sessionToken.getToken())).thenReturn(false);
        when(activeOrderService.payment(paymentGateway, any(), any())).thenReturn(true);

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
        when(authenticationService.validateToken(sessionToken.getToken())).thenReturn(true);
        when(activeOrderService.payment(paymentGateway, any(), any())).thenReturn(false);
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
        when(authenticationService.validateToken(sessionToken.getToken())).thenReturn(true);
        when(activeOrderService.payment(paymentGateway, any(), any())).thenReturn(true);
        verify(activeOrderRepoMock, times(0)).delete(orderId);

      
        assertThrows(Exception.class, () ->activeOrderService.completeOrder(paymentGateway, sessionToken, 100, orderId));        
    } 
    @Test
    public void givenValidOrder_whenUpdateOrder_thenReturnUpdatedOrder() {
    ActiveOrderItem order = mock(ActiveOrderItem.class);
    SessionToken sessionToken = mock(SessionToken.class);
    when(order.getOrderId()).thenReturn("order1");
    when(activeOrderRepoMock.findById(order.getOrderId())).thenReturn(order);
    when(sessionToken.getToken()).thenReturn("user");
    when(authenticationService.validateToken(sessionToken.getToken())).thenReturn(true);
    when(order.getQuantity()).thenReturn(5);
    int newQuantity = 10;
    when(activeOrderPublisher.publishReserveTickets(order.getOrderId(), newQuantity - order.getQuantity())).thenReturn(true);
    verify(order).setQuantity(newQuantity);
    verify(activeOrderRepoMock, times(0)).update(order);
    }
    @Test
    public void givenNonExistentOrder_whenUpdateOrder_thenThrowException(){
        SessionToken sessionToken = mock(SessionToken.class);
        String orderId = "nonExistingOrder";
        int quantity = 5;
        when(activeOrderRepoMock.findById(orderId)).thenReturn(null);
        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validateToken(sessionToken.getToken())).thenReturn(true);
        assertThrows(Exception.class, ()-> activeOrderService.updateActiveOrder(sessionToken, orderId,quantity));

    }
    @Test
    public void givenExpiredSessionToken_whenUpdateOrder_thenThrowException(){
        SessionToken sessionToken = mock(SessionToken.class);
        ActiveOrderItem order = mock(ActiveOrderItem.class);
        String orderId = "order1";
        int quantity = 5;
        when(order.getOrderId()).thenReturn(orderId);
        when(activeOrderRepoMock.findById(order.getOrderId())).thenReturn(order);
        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validateToken(sessionToken.getToken())).thenReturn(false);
        assertThrows(Exception.class, ()-> activeOrderService.updateActiveOrder(sessionToken, orderId,quantity));
    }
    @Test
    public void givenInvalidQuantity_whenUpdateOrder_thenThrowException(){
        SessionToken sessionToken = mock(SessionToken.class);
        ActiveOrderItem order = mock(ActiveOrderItem.class);
        String orderId = "order1";
        int quantity = -1;
        when(order.getOrderId()).thenReturn(orderId);
        when(activeOrderRepoMock.findById(order.getOrderId())).thenReturn(order);
        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validateToken(sessionToken.getToken())).thenReturn(true);
        assertThrows(Exception.class, ()-> activeOrderService.updateActiveOrder(sessionToken, orderId,quantity));
    } 
    //TODO: need to add tests for valid quantities when we have seating maps
    @Test
    public void givenExpiredActiveOrder_whenUpdateOrder_thenThrowException(){
        SessionToken sessionToken = mock(SessionToken.class);
        ActiveOrderItem order = mock(ActiveOrderItem.class);
        String orderId = "order1";
        int quantity = 5;
        String eventId = "event1";
        when(order.getCreatedAt()).thenReturn(new Timestamp(0));

        when(order.getOrderId()).thenReturn(orderId);
        when(order.getQuantity()).thenReturn(quantity);
        when(order.getEventId()).thenReturn(eventId);

        when(activeOrderPublisher.publishReserveTickets(eventId, quantity)).thenReturn(true);
        //when(activeOrderPublisher.publishUnreserveTickets(eventId, any())).thenReturn(true);
        when(activeOrderRepoMock.findById(order.getOrderId())).thenReturn(order);
        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validateToken(sessionToken.getToken())).thenReturn(true);
        assertThrows(Exception.class, ()-> activeOrderService.updateActiveOrder(sessionToken, orderId,quantity));
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
        when(order.getQuantity()).thenReturn(quantity);
        when(order.getEventId()).thenReturn(eventId);

        when(activeOrderPublisher.publishReserveTickets(eventId, quantity)).thenReturn(false);
        //when(activeOrderPublisher.publishUnreserveTickets(eventId, any())).thenReturn(true);
        when(activeOrderRepoMock.findById(order.getOrderId())).thenReturn(order);
        when(sessionToken.getToken()).thenReturn("user");
        when(authenticationService.validateToken(sessionToken.getToken())).thenReturn(true);
        assertThrows(Exception.class, ()-> activeOrderService.updateActiveOrder(sessionToken, orderId,newQuantity));
    }
}
