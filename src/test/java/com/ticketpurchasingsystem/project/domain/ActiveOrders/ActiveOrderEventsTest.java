package com.ticketpurchasingsystem.project.domain.ActiveOrders;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.*;
import org.junit.jupiter.api.Test;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class ActiveOrderEventsTest {

    @Test
    public void GivenOrderWithMixedSeatIds_WhenCreatingIsUpToPolicyEvent_ThenGettersReturnCorrectValues() {
        Object source = new Object();
        List<String> seatIds = new ArrayList<>();
        seatIds.add("A1");
        seatIds.add("");
        seatIds.add(null);
        seatIds.add("B2");

        HashMap<String, Integer> standingQuantities = new HashMap<>();
        standingQuantities.put("StandingArea1", 3);
        standingQuantities.put("StandingArea2", 5);

        ActiveOrderDTO order = new ActiveOrderDTO("order1", "user1", "event1", new Timestamp(System.currentTimeMillis()), seatIds, standingQuantities);

        IsUpToPolicyEvent event = new IsUpToPolicyEvent(source, order, 25);

        assertEquals(order, event.getOrder());
        assertEquals("event1", event.getEventID());
        assertEquals("user1", event.userID());
        assertEquals(seatIds, event.getSeatIds());
        assertEquals(standingQuantities, event.getStandingAreaQuantities());
        assertEquals(25, event.getAge());

        // Test getTotalTickets()
        assertEquals(12, event.getTotalTickets());

        // Test isSeatEmpty()
        assertFalse(event.isSeatEmpty());

        // Test isSeatEmpty() when all seats are null or empty
        List<String> emptySeats = new ArrayList<>();
        emptySeats.add(null);
        emptySeats.add("");
        ActiveOrderDTO emptyOrder = new ActiveOrderDTO("order1", "user1", "event1", new Timestamp(System.currentTimeMillis()), emptySeats, standingQuantities);
        IsUpToPolicyEvent emptyEvent = new IsUpToPolicyEvent(source, emptyOrder, 25);
        assertTrue(emptyEvent.isSeatEmpty());

        // Test getResult() before setting result throws NPE
        assertThrows(NullPointerException.class, () -> event.getResult());

        // Test getResult() after setting result
        event.setResult(true);
        assertTrue(event.getResult());

        event.setResult(false);
        assertFalse(event.getResult());
    }

    @Test
    public void GivenValidSeatIds_WhenCreatingSeatReleaseEvent_ThenGettersReturnCorrectValues() {
        Object source = new Object();
        List<String> seatIds = List.of("A1", "A2");
        SeatReleaseEvent event = new SeatReleaseEvent(source, "token123", "event1", seatIds, "order1");

        assertEquals("event1", event.getEventID());
        assertEquals(seatIds, event.getSeatIds());
        assertEquals("order1", event.getOrderID());
        assertEquals("token123", event.getSessionToken());
    }

    @Test
    public void GivenValidSeatIds_WhenCreatingSeatReservationEvent_ThenGettersAndResultReturnCorrectValues() {
        Object source = new Object();
        List<String> seatIds = List.of("B1");
        SeatReservationEvent event = new SeatReservationEvent(source, "token123", "event1", seatIds, "order1");

        assertEquals(seatIds, event.getSeatIds());
        assertEquals("event1", event.getEventID());
        assertEquals("order1", event.getOrderID());
        assertEquals("token123", event.getSessionToken());
        assertNull(event.getResult());

        event.setResult(true);
        assertTrue(event.getResult());
    }

    @Test
    public void GivenValidSeatIds_WhenCreatingCheckSeatsReservedEvent_ThenGettersAndResultReturnCorrectValues() {
        Object source = new Object();
        List<String> seatIds = List.of("C1");
        CheckSeatsReservedEvent event = new CheckSeatsReservedEvent(source, "token123", "order1", "event1", seatIds);

        assertEquals("token123", event.getSessionToken());
        assertEquals("order1", event.getOrderId());
        assertEquals("event1", event.getEventId());
        assertEquals(seatIds, event.getSeatIds());
        assertNull(event.getResult());

        List<String> result = List.of("C1");
        event.setResult(result);
        assertEquals(result, event.getResult());
    }

    @Test
    public void GivenUserId_WhenCreatingIsMemberEvent_ThenDefaultIsFalseAndCanBeSet() {
        Object source = new Object();
        IsMemberEvent event = new IsMemberEvent(source, "user1");

        assertEquals("user1", event.getUserId());
        assertFalse(event.getResult());

        event.setAnswer(true);
        assertTrue(event.getResult());
    }

    @Test
    public void GivenValidAreaAndQuantity_WhenCreatingStandingAreaReservationEvent_ThenGettersReturnCorrectValues() {
        Object source = new Object();
        StandingAreaReservationEvent event = new StandingAreaReservationEvent(source, "token123", "event1", "area1", 5);

        assertEquals("event1", event.getEventId());
        assertEquals("area1", event.getAreaId());
        assertEquals(5, event.getQuantity());
        assertEquals("token123", event.getSessionToken());
        assertNull(event.getResult());

        event.setResult(true);
        assertTrue(event.getResult());
    }

    @Test
    public void GivenUserAndOrderId_WhenCreatingOrderCancelledEvent_ThenGettersReturnCorrectValues() {
        Object source = new Object();
        OrderCancelledEvent event = new OrderCancelledEvent(source, "user1", "order1");

        assertEquals("user1", event.getUserId());
        assertEquals("order1", event.getOrderId());
    }

    @Test
    public void GivenNoInput_WhenCreatingNewActiveOrder_ThenObjectIsNotNull() {
        NewActiveOrder event = new NewActiveOrder();
        assertNotNull(event);
    }

    @Test
    public void GivenValidAreaAndQuantity_WhenCreatingStandingAreaReleaseEvent_ThenGettersReturnCorrectValues() {
        Object source = new Object();
        StandingAreaReleaseEvent event = new StandingAreaReleaseEvent(source, "token123", "event1", "area1", 10);

        assertEquals("event1", event.getEventID());
        assertEquals("area1", event.getAreaID());
        assertEquals(10, event.getQuantity());
        assertEquals("token123", event.getSessionToken());
    }

    @Test
    public void GivenUserOrderAndAmount_WhenCreatingOrderRefundedEvent_ThenGettersReturnCorrectValues() {
        Object source = new Object();
        OrderRefundedEvent event = new OrderRefundedEvent(source, "user1", "order1", 150.0);

        assertEquals("user1", event.getUserId());
        assertEquals("order1", event.getOrderId());
        assertEquals(150.0, event.getAmount());
    }

    @Test
    public void GivenOrderAndPaymentDetails_WhenCreatingCompletedOrderEvent_ThenGettersReturnCorrectValues() {
        Object source = new Object();
        ActiveOrderDTO order = new ActiveOrderDTO("order1", "user1", "event1", new Timestamp(System.currentTimeMillis()), List.of("A1"), new HashMap<>());
        CompletedOrderEvent event = new CompletedOrderEvent(source, order, 250.0, 99);

        assertEquals(order, event.getOrder());
        assertEquals(250.0, event.getAmountPaid());
        assertEquals(99, event.getCompanyId());
    }

    @Test
    public void GivenEventId_WhenCreatingGetCompanyIdEvent_ThenResultCanBeSetAndRetrieved() {
        Object source = new Object();
        GetCompanyIdEvent event = new GetCompanyIdEvent(source, "event1");

        assertEquals("event1", event.getEventId());
        assertNull(event.getResult());

        event.setResult(55);
        assertEquals(55, event.getResult());
    }

    @Test
    public void GivenEventId_WhenCreatingIsValidEventIDEvent_ThenDefaultIsFalseAndCanBeSet() {
        Object source = new Object();
        IsValidEventIDEvent event = new IsValidEventIDEvent(source, "event1");

        assertEquals("event1", event.getEventId());
        assertFalse(event.isValid());

        event.setResult(true);
        assertTrue(event.isValid());
    }
}
