package com.ticketpurchasingsystem.project.domain.Events;

import com.ticketpurchasingsystem.project.domain.event.Event;
import com.ticketpurchasingsystem.project.domain.event.EventDiscountPolicy;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.EventPurchasePolicy;
import com.ticketpurchasingsystem.project.domain.event.Maps.AssignedSeat;
import com.ticketpurchasingsystem.project.domain.event.Maps.StandingArea;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingMap;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.tickets.ITicketPurchaseRule;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class LegacyEventTests {

    @Test
    public void GivenAssignedSeat_WhenBookingAndUnbooking_ThenSeatStateChangesCorrectly() {
        AssignedSeat seat = new AssignedSeat("VIP", 1, 5, 100.0);
        assertEquals("VIP_1_5", seat.getId());
        assertFalse(seat.isBooked());
        assertEquals(100.0, seat.getPriceForTicket());

        // isbooked orderId
        assertFalse(seat.isbooked(null));
        assertFalse(seat.isbooked("order123"));

        // book invalid quantity
        assertFalse(seat.book("order123", 2));

        // book success
        assertTrue(seat.book("order123", 1));
        assertTrue(seat.isBooked());
        assertTrue(seat.isbooked("order123"));
        assertFalse(seat.isbooked("order456"));

        // book already booked
        assertFalse(seat.book("order456", 1));

        // unbook invalid quantity
        assertFalse(seat.unbook(2));

        // unbook success
        assertTrue(seat.unbook(1));
        assertFalse(seat.isBooked());

        // unbook not booked seat
        assertFalse(seat.unbook(1));

        // set price
        assertFalse(seat.setPriceForTicket(-5.0));
        assertTrue(seat.setPriceForTicket(120.0));
        assertEquals(120.0, seat.getPriceForTicket());
    }

    @Test
    public void GivenStandingArea_WhenBookingAndUnbooking_ThenAvailabilityChangesCorrectly() {
        StandingArea area = new StandingArea(100, 50.0, "floor1");
        assertEquals("floor1", area.getId());
        assertEquals(100, area.getAvalibleSeatNumber());
        assertEquals(50.0, area.getPriceForTicket());
        assertTrue(area.isbooked("order123"));

        // book standing
        assertTrue(area.book("order123", 40));
        assertEquals(60, area.getAvalibleSeatNumber());

        // book standing too many
        assertFalse(area.book("order123", 70));
        assertEquals(60, area.getAvalibleSeatNumber());

        // unbook standing invalid
        assertFalse(area.unbook(0));
        assertFalse(area.unbook(-10));
        assertFalse(area.unbook(41)); // exceeds capacity (60 + 41 = 101 > 100)

        // unbook success
        assertTrue(area.unbook(30));
        assertEquals(90, area.getAvalibleSeatNumber());

        // set price
        assertFalse(area.setPriceForTicket(-10.0));
        assertTrue(area.setPriceForTicket(45.0));
        assertEquals(45.0, area.getPriceForTicket());
    }

    @Test
    public void GivenSeatingMap_WhenAddingAreasAndBookingSeats_ThenMapBehavesCorrectly() {
        SeatingMap map = new SeatingMap();

        // invalid adds
        assertFalse(map.addStandingArea(0, 10.0));
        assertFalse(map.addStandingArea(10, -5.0));
        assertFalse(map.addSeatingArea(0, 5, 20.0));
        assertFalse(map.addSeatingArea(2, 0, 20.0));
        assertFalse(map.addSeatingArea(2, 2, -10.0));

        // valid adds
        assertTrue(map.addStandingArea(100, 50.0)); // areaID will be "0"
        assertTrue(map.addSeatingArea(2, 3, 80.0));    // areaID will be "1", seat IDs like "1_1_1", "1_1_2", etc.

        assertNotNull(map.getArea("0"));
        assertNull(map.getArea("non-existent"));
        assertNotNull(map.getSeat("1_1_1"));
        assertNull(map.getSeat("non-existent"));

        // lists
        assertEquals(1, map.getAreaIds().size());
        assertEquals(6, map.getSeatIds().size());
//        assertEquals(7, map.getPurchaseAreas().size());

        // book standing area
        assertFalse(map.bookStandingArea("non-existent", "order1", 5));
        assertTrue(map.bookStandingArea("0", "order1", 10));

        // unbook standing area
        assertFalse(map.unbookStandingArea("non-existent", 5));
        assertTrue(map.unbookStandingArea("0", 5));

        // book assigned seats
        assertFalse(map.bookAssignedSeats(List.of("non-existent"), "order1"));
        assertTrue(map.bookAssignedSeats(List.of("1_1_1", "1_1_2"), "order1"));

        // book assigned seats rollback logic
        // Let's make "1_2_1" booked by order2
        assertTrue(map.bookAssignedSeats(List.of("1_2_1"), "order2"));
        // Now try to book "1_2_2" and "1_2_1" for order3. It should book 1_2_2, fail on 1_2_1, and rollback 1_2_2.
        assertThrows(IllegalStateException.class, () -> {
            map.bookAssignedSeats(List.of("1_2_2", "1_2_1"), "order3");
        });
        // Verify 1_2_2 is rolled back (not booked)
        assertFalse(map.getSeat("1_2_2").isBooked());

        // unbook assigned seats
        assertThrows(IllegalArgumentException.class, () -> {
            map.unbookAssignedSeats(List.of("non-existent"));
        });
        assertThrows(IllegalStateException.class, () -> {
            map.unbookAssignedSeats(List.of("1_2_2")); // 1_2_2 is not booked, so unbook(1) returns false
        });
        assertTrue(map.unbookAssignedSeats(List.of("1_1_1")));

        // prices
        assertFalse(map.editPriceForTicket("0", -5.0));
        assertTrue(map.editPriceForTicket("0", 60.0));
        assertEquals(60.0, map.getPriceForTicket("0"));

        assertTrue(map.editPriceForTicket("1_1_2", 90.0));
        assertEquals(90.0, map.getPriceForTicket("1_1_2"));

        assertFalse(map.editPriceForTicket("non-existent", 10.0));
        assertEquals(-1.0, map.getPriceForTicket("non-existent"));

        // remove area
        assertTrue(map.removeArea("0"));
        assertTrue(map.removeArea("1_1_3"));
        assertFalse(map.removeArea("non-existent"));
    }

    @Test
    public void GivenEventWithVariousParameters_WhenSettingFieldsAndCopying_ThenEventBehavesCorrectly() {
        LocalDateTime date = LocalDateTime.now().plusDays(2);
        EventPurchasePolicy purchasePolicy = new EventPurchasePolicy();
        EventDiscountPolicy discountPolicy = new EventDiscountPolicy(List.of());

        // constructor validations
        assertThrows(IllegalArgumentException.class, () -> new Event(1, null, 100, date, purchasePolicy, discountPolicy, 1));
        assertThrows(IllegalArgumentException.class, () -> new Event(1, "   ", 100, date, purchasePolicy, discountPolicy, 1));
        assertThrows(IllegalArgumentException.class, () -> new Event(1, "Name", 0, date, purchasePolicy, discountPolicy, 1));
        assertThrows(IllegalArgumentException.class, () -> new Event(1, "Name", -5, date, purchasePolicy, discountPolicy, 1));
        assertThrows(IllegalArgumentException.class, () -> new Event(1, "Name", 100, null, purchasePolicy, discountPolicy, 1));

        // valid construction
        Event event = new Event(1, "Concert", 1000, date, purchasePolicy, discountPolicy, 0);
        assertEquals(1, event.getCompanyId());
        assertEquals("Concert", event.getEventName());
        assertEquals(1000, event.getEventCapacity());
        assertEquals(date, event.getEventDate());
        assertEquals(purchasePolicy, event.getPurchasePolicy());
        assertTrue(event.isActive());
        assertEquals(0, event.getVersion());

        // setters & copy
        event.setEventId("event-123");
        assertEquals("event-123", event.getEventId());

        LocalDateTime newDate = date.plusDays(1);
        event.setEventDate(newDate);
        assertEquals(newDate, event.getEventDate());

        SeatingMap map = new SeatingMap();
        event.setSeatingMap(map);
        assertEquals(map, event.getSeatingMap());

        event.setVersion(5);
        assertEquals(5, event.getVersion());

        event.setEventCapacity(1500);
        assertEquals(1500, event.getEventCapacity());

        ITicketPurchaseRule rule = mock(ITicketPurchaseRule.class);
        event.setTicketPurchasePolicy(rule);
        assertEquals(rule, event.getTicketPurchasePolicy());

        // setPurchasePolicy DTO
        PurchasePolicyDTO dto = new PurchasePolicyDTO(1, 10, false, 18, 70, true, false);
        event.setPurchasePolicy(dto);
        assertNotNull(event.getPurchasePolicy());

        // copy constructor
        Event copy = new Event(event);
        assertEquals("event-123", copy.getEventId());
        assertEquals(1, copy.getCompanyId());
        assertEquals("Concert", copy.getEventName());
        assertEquals(1500, copy.getEventCapacity());
        assertTrue(copy.isActive());
        assertEquals(newDate, copy.getEventDate());
        assertEquals(map, copy.getSeatingMap());
        assertEquals(5, copy.getVersion());
        assertEquals(rule, copy.getTicketPurchasePolicy());
    }
}
