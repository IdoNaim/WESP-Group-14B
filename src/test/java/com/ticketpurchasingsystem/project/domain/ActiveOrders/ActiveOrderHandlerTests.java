package com.ticketpurchasingsystem.project.domain.ActiveOrders;

import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ActiveOrderHandlerTests {

    private static final long EXPIRATION_TIME_MS = 15 * 60 * 1000L; // 15 minutes in milliseconds

    private ActiveOrderHandler handler;
    private ActiveOrderItem mockOrder;

    @BeforeEach
    public void setUp() {
        handler = new ActiveOrderHandler();
        mockOrder = mock(ActiveOrderItem.class);
    }

    //----------------------------------------
    // helper functions
    //-----------------------------------------

    public boolean sameMap(Map<String, Integer> map1, Map<String, Integer> map2){
        boolean result = true;
        for(Map.Entry<String, Integer> entry : map1.entrySet()){
            boolean inMap2 = map2.keySet().contains(entry.getKey()) &&
                    map2.get(entry.getKey()).equals(entry.getValue());
            if(!inMap2){
                return false;
            }
        }
        for(Map.Entry<String, Integer> entry : map2.entrySet()){
            boolean inMap1 = map1.keySet().contains(entry.getKey()) &&
                    map1.get(entry.getKey()).equals(entry.getValue());
            if(!inMap1){
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------
    // Get Active Order Info Tests
    // ---------------------------------------------------------

    @Test
    public void GivenMatchingUserId_WhenGetActiveOrderInfo_ThenReturnDto() {
        String userId = "user123";
        String orderId = "9999";
        String eventId = "event1";
        Timestamp time = new Timestamp(System.currentTimeMillis());
        List<String> seats = List.of("seat1");
        HashMap<String,Integer> standingTickets= new HashMap<>();
        standingTickets.put("area1", 1);

        when(mockOrder.getUserId()).thenReturn(userId);
        when(mockOrder.getOrderId()).thenReturn(orderId);
        when(mockOrder.getEventId()).thenReturn(eventId);
        when(mockOrder.getCreatedAt()).thenReturn(time);
        when(mockOrder.getSeatIds()).thenReturn(seats);
        when(mockOrder.getStandingAreaQuantities()).thenReturn(standingTickets);

        ActiveOrderDTO result = handler.getActiveOrderInfo(userId, mockOrder);

        assertNotNull(result);
        assertEquals(result.getOrderId(), orderId);
        assertEquals(result.getUserId(), userId);
        assertEquals(result.getEventId(), eventId);
        assertEquals(result.getSeatIds(), seats);
        assertTrue(sameMap(result.getStandingAreaQuantities(), standingTickets));
        assertEquals(result.getCreatedAt().getTime(), time.getTime());
    }

    @Test
    public void GivenMismatchedUserId_WhenGetActiveOrderInfo_ThenReturnNull() {
        when(mockOrder.getUserId()).thenReturn("user123");
        when(mockOrder.getOrderId()).thenReturn("9999");

        ActiveOrderDTO result = handler.getActiveOrderInfo("differentUser", mockOrder);
        assertNull(result);
    }

    @Test
    public void GivenNullParametersOrFields_WhenGetActiveOrderInfo_ThenReturnNull() {
        assertNull(handler.getActiveOrderInfo(null, mockOrder));
        assertNull(handler.getActiveOrderInfo("user123", null));

        when(mockOrder.getUserId()).thenReturn(null);
        assertNull(handler.getActiveOrderInfo("user123", mockOrder));

        when(mockOrder.getUserId()).thenReturn("user123");
        when(mockOrder.getOrderId()).thenReturn(null);
        assertNull(handler.getActiveOrderInfo("user123", mockOrder));
    }

    // ---------------------------------------------------------
    // Is Users Order Tests
    // ---------------------------------------------------------

    @Test
    public void GivenUserOwnsOrder_WhenIsUsersOrder_ThenReturnTrue() {
        when(mockOrder.getUserId()).thenReturn("user123");
        assertTrue(handler.isUsersOrder("user123", mockOrder));
    }

    @Test
    public void GivenUserDoesNotOwnOrder_WhenIsUsersOrder_ThenReturnFalse() {
        when(mockOrder.getUserId()).thenReturn("user123");
        assertFalse(handler.isUsersOrder("wrongUser", mockOrder));
    }

    // ---------------------------------------------------------
    // Collection Release Tests
    // ---------------------------------------------------------

    @Test
    public void GivenValidSeatsList_WhenCanReleaseSeats_ThenReturnTrue() {
        assertTrue(handler.canReleaseSeats(List.of("A1", "A2")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void GivenNullOrEmptySeatsList_WhenCanReleaseSeats_ThenReturnFalse(List<String> seats) {
        assertFalse(handler.canReleaseSeats(seats));
    }

    @Test
    public void GivenValidStandingMap_WhenCanReleaseStanding_ThenReturnTrue() {
        assertTrue(handler.canReleaseStanding(Map.of("ZoneA", 5)));
    }

    @Test
    public void GivenNullOrEmptyStandingMap_WhenCanReleaseStanding_ThenReturnFalse() {
        assertFalse(handler.canReleaseStanding(null));
        assertFalse(handler.canReleaseStanding(Collections.emptyMap()));
    }

    // ---------------------------------------------------------
    // Create Active Order Validation Tests
    // ---------------------------------------------------------

    @Test
    public void GivenValidOrder_WhenCanCreateActiveOrder_ThenReturnTrue() {
        when(mockOrder.getOrderId()).thenReturn("12345");
        assertTrue(handler.canCreateActiveOrder(mockOrder));
    }

    @Test
    public void GivenNullOrder_WhenCanCreateActiveOrder_ThenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                handler.canCreateActiveOrder(null)
        );
    }

    @Test
    public void GivenInvalidOrderId_WhenCanCreateActiveOrder_ThenThrowIllegalArgumentException() {
        when(mockOrder.getOrderId()).thenReturn("-5");
        assertThrows(IllegalArgumentException.class, () ->
                handler.canCreateActiveOrder(mockOrder)
        );
    }

    // ---------------------------------------------------------
    // Order Id Validation Tests
    // ---------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "999999999999"})
    public void GivenValidOrderIdString_WhenIsValidOrderID_ThenReturnTrue(String validId) {
        assertTrue(handler.isValidOrderID(validId));
    }

    @Test
    public void GivenNullOrderId_WhenIsValidOrderID_ThenReturnFalse() {
        assertFalse(handler.isValidOrderID(null));
    }

    @Test
    public void GivenNegativeOrderId_WhenIsValidOrderID_ThenReturnFalse() {
        assertFalse(handler.isValidOrderID("-123"));
    }

    @Test
    public void GivenNonNumericOrderId_WhenIsValidOrderID_ThenThrowNumberFormatException() {
        assertThrows(NumberFormatException.class, () ->
                handler.isValidOrderID("abc")
        );
    }

    // ---------------------------------------------------------
    // Modification Tests
    // ---------------------------------------------------------

    @Test
    public void GivenNullOrder_WhenAddSeatsToActiveOrder_ThenReturnNull() {
        assertNull(handler.addSeatsToActiveOrder(mockOrder, null));
    }
    @Test
    public void GivenNullSeatList_WhenAddSeatsToActiveOrder_ThenReturnNull(){
        assertNull(handler.addSeatsToActiveOrder(mockOrder, null));
    }

    @Test
    public void GivenNullOrder_WhenAddStandingAreaToActiveOrder_ThenReturnNull() {
        assertNull(handler.addStandingAreaToActiveOrder(null, "ZoneA", 2));
    }
    @Test
    public void GivenNullArea_WhenAddStandingAreaToActiveOrder_ThenReturnNull(){
        assertNull(handler.addStandingAreaToActiveOrder(mockOrder, null, 2));
    }
    @Test
    public void GivenNegativeQuantity_WhenAddStandingAreaToActiveOrder_ThenReturnNull() {
        assertNull(handler.addStandingAreaToActiveOrder(mockOrder, "ZoneA", -1));
    }

    @Test
    public void GivenZeroQuantity_WhenAddStandingAreaToActiveOrder_ThenReturnSameOrderInstance() {
        ActiveOrderItem result = handler.addStandingAreaToActiveOrder(mockOrder, "ZoneA", 0);
        assertSame(mockOrder, result);
    }

    // ---------------------------------------------------------
    // Expiration Tests
    // ---------------------------------------------------------

    @Test
    public void GivenPastCreationDate_WhenIsOrderExpired_ThenReturnTrue() {
        // Created outside the 15-minute window (e.g., 16 minutes ago)
        Timestamp pastDate = new Timestamp(System.currentTimeMillis() - (EXPIRATION_TIME_MS + (60 * 1000L)));
        when(mockOrder.getCreatedAt()).thenReturn(pastDate);

        assertTrue(handler.isOrderExpired(mockOrder));
    }

    @Test
    public void GivenRecentCreationDate_WhenIsOrderExpired_ThenReturnFalse() {
        // Created comfortably within the 15-minute window (e.g., right now)
        Timestamp now = new Timestamp(System.currentTimeMillis());
        when(mockOrder.getCreatedAt()).thenReturn(now);

        assertFalse(handler.isOrderExpired(mockOrder));
    }

    @Test
    public void GivenDtoWithRecentCreationDate_WhenIsOrderExpired_ThenReturnFalse(){
        ActiveOrderDTO mockDto = mock(ActiveOrderDTO.class);
        // Created outside the 15-minute window (e.g., 16 minutes ago)
        Timestamp now = new Timestamp(System.currentTimeMillis());
        when(mockDto.getCreatedAt()).thenReturn(now);

        assertFalse(handler.isOrderExpired(mockDto));
    }
    @Test
    public void GivenDtoWithPastCreationDate_WhenIsOrderExpired_ThenReturnTrue() {
        ActiveOrderDTO mockDto = mock(ActiveOrderDTO.class);
        // Created outside the 15-minute window (e.g., 16 minutes ago)
        Timestamp pastDate = new Timestamp(System.currentTimeMillis() - (EXPIRATION_TIME_MS + (60 * 1000L)));
        when(mockDto.getCreatedAt()).thenReturn(pastDate);

        assertTrue(handler.isOrderExpired(mockDto));
    }

    // ---------------------------------------------------------
    // Collection Manipulation Tests
    // ---------------------------------------------------------

    @Test
    public void GivenNewAndCurrentSeats_WhenGetSeatsToReserve_ThenReturnMissingSeats() {
        List<String> currentSeats = List.of("A1", "A2");
        List<String> newSeats = List.of("A2", "A3", "A4");

        List<String> toReserve = handler.getSeatsToReserve(currentSeats, newSeats);

        assertEquals(2, toReserve.size());
        assertTrue(toReserve.containsAll(List.of("A3", "A4")));
    }

    @Test
    public void GivenNewAndCurrentSeats_WhenGetSeatsToRelease_ThenReturnRemovedSeats() {
        List<String> currentSeats = List.of("A1", "A2", "A3");
        List<String> newSeats = List.of("A2");

        List<String> toRelease = handler.getSeatsToRelease(currentSeats, newSeats);

        assertEquals(2, toRelease.size());
        assertTrue(toRelease.containsAll(List.of("A1", "A3")));
    }

    @Test
    public void GivenNewAndCurrentStanding_WhenCalculateStandingToReserve_ThenReturnPositiveDifferences() {
        Map<String, Integer> currentStanding = Map.of("ZoneA", 2, "ZoneB", 5);
        Map<String, Integer> newStanding = Map.of("ZoneA", 5, "ZoneB", 3, "ZoneC", 2);

        Map<String, Integer> toReserve = handler.calculateStandingToReserve(currentStanding, newStanding);

        // ZoneA needs 3 more, ZoneB needs less (ignored), ZoneC needs 2 more
        assertEquals(2, toReserve.size());
        assertEquals(3, toReserve.get("ZoneA"));
        assertEquals(2, toReserve.get("ZoneC"));
        assertFalse(toReserve.containsKey("ZoneB"));
    }
}