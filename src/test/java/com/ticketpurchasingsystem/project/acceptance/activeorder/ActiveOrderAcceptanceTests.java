package com.ticketpurchasingsystem.project.acceptance.activeorder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.IActiveOrderService;
import com.ticketpurchasingsystem.project.application.IBarCodeGateway;
import com.ticketpurchasingsystem.project.application.IEventService;
import com.ticketpurchasingsystem.project.application.IPaymentGateway;
import com.ticketpurchasingsystem.project.application.PaymentDetails;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.BarcodeDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.IActiveOrderRepo;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingAreaConfig;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingMap;
import com.ticketpurchasingsystem.project.domain.event.Maps.StandingAreaConfig;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;

@SpringBootTest
@ActiveProfiles("test")
public class ActiveOrderAcceptanceTests {

    @Autowired
    private IActiveOrderService activeOrderService;

    @Autowired
    private IActiveOrderRepo activeOrderRepo;

    @Autowired
    private IEventService eventService;

    @Autowired
    private IEventRepo eventRepo;

    @Autowired
    private IHistoryOrderRepo historyOrderRepo;

    @Autowired
    private AuthenticationService authenticationService;

    @MockBean
    private IBarCodeGateway barcodeGatewayMock;

    private IPaymentGateway paymentGatewayMock;

    private EventDTO testEvent;
    private SeatingMap seatingMap;
    private List<String> seatIds;
    private List<String> standingAreas;

    private final Set<String> registeredUsers = new HashSet<>();

    private static final String USER1_ID = "123";
    private static final String USER2_ID = "456";
    private int companyId = 10;
    private String eventName = "EventA";
    private Integer eventCapacity = 50;
    private PurchasePolicyDTO purchasePolicyDTO = new PurchasePolicyDTO(0, eventCapacity, false, 0, 60, true, false);
    private List<DiscountDTO> discounts = new ArrayList<>();
    private LocalDateTime eventDate = LocalDateTime.of(LocalDate.now().plusYears(1), LocalTime.now());
    private String eventLocation = "test location";

    private PaymentDetails paymentDetailsFor(double amount) {
        return new PaymentDetails(amount, "USD", "4111111111111111", "12", "2028", "Test User", "123", "ID-001");
    }

    @BeforeEach
    public void setup() {
        registeredUsers.clear();
        paymentGatewayMock = Mockito.mock(IPaymentGateway.class);

        String sessionToken = authenticationService.login(USER2_ID);

        EventDTO e = new EventDTO(
                null,
                companyId,
                eventName,
                eventCapacity,
                eventDate,
                true,
                eventLocation,
                null,
                null,
                null
        );
        eventService.createEvent(sessionToken, e, purchasePolicyDTO, discounts);
        List<EventDTO> events = eventService.searchEventsByCompany(sessionToken, companyId);
        testEvent = events.getFirst();
        this.seatingMap = eventService.configureSeatingMap(sessionToken, List.of(new SeatingAreaConfig(1, 2, 10)), List.of(new StandingAreaConfig(20, 10)));
        seatIds = new ArrayList<>(seatingMap.getSeatIds());
        standingAreas = new ArrayList<>(seatingMap.getAreaIds());
        eventService.editEventSeatingMap(sessionToken, testEvent.eventId(), seatingMap);
    }

    @AfterEach
    public void tearDown() {
        activeOrderRepo.deleteAll();
        eventRepo.deleteAll();
    }

    @Test
    public void givenTestEnvironmentSetup_whenCheckingSetup_thenAssertionSucceeds() {
        assertTrue(true);
        System.out.println("setup complete!");
    }

    // UC II.2.5
    @Test
    public void GivenValidEventId_WhenCreateOrder_ThenCreateActiveOrderSuccessfully() {
        String sessionToken = authenticationService.login(USER1_ID);

        ActiveOrderItem order = activeOrderService.createPendingOrder(new SessionToken(sessionToken, 1000), USER1_ID, testEvent.eventId());
        assertNotNull(order);
        assertEquals(order.getEventId(), testEvent.eventId());
        assertEquals(order.getUserId(), USER1_ID);
        assertNotNull(order.getCreatedAt());
    }

    @Test
    public void GivenUserAlreadyHasActiveOrder_WhenCreateOrder_ThenThrowException() {
        String sessionToken = authenticationService.login(USER1_ID);
        SessionToken session = new SessionToken(sessionToken, 1000);

        ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
        if (order == null) {
            fail("couldnt create first active order, should have worked");
        }
        assertThrows(Exception.class, () -> activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId()));
    }

    @Test
    public void GivenEventDoesntExist_WhenCreateOrder_ThenThrowException() {
        String sessionToken = authenticationService.login(USER1_ID);
        SessionToken session = new SessionToken(sessionToken, 1000);

        assertThrows(Exception.class, () -> activeOrderService.createPendingOrder(session, USER1_ID, "nonExistentId"));
    }

    // UC II.2.6
    @Test
    public void GivenValidSeatIds_WhenAddSeatsToActiveOrder_ThenAddSeatsToOrder() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());

            if (order == null) {
                fail("couldnt create active order, should have worked");
            }

            assertDoesNotThrow(() -> activeOrderService.addSeatsToActiveOrder(session, order.getOrderId(), seatIds));
            ActiveOrderDTO updatedOrder = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            assertEquals(updatedOrder.getSeatIds(), seatIds);
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenValidSeatIds_WhenAddSeatsToActiveOrder_ThenSeatsAreReservedForOrder() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());

            if (order == null) {
                fail("couldnt create active order, should have worked");
            }

            assertDoesNotThrow(() -> activeOrderService.addSeatsToActiveOrder(session, order.getOrderId(), seatIds));
            assertTrue(eventService.checkSeatsReserved(sessionToken, order.getOrderId(), order.getEventId(), seatIds).isEmpty());
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenSeatsThatDontExist_WhenAddSeatsToActiveOrder_ThenSeatsNotAddedToOrder() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (order == null) {
                fail("couldnt create active order, should have worked");
            }
            String nonExistingSeatId = "NonExistentId";
            assertThrows(Exception.class, () -> activeOrderService.addSeatsToActiveOrder(session, order.getOrderId(), List.of(nonExistingSeatId)));
            ActiveOrderDTO orderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            assertFalse(orderDTO.getSeatIds().contains(nonExistingSeatId));
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenSeatsAlreadyReserved_WhenAddSeatsToActiveOrder_ThenOrderDoesntHaveTheSeats() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (order == null) {
                fail("couldnt create active order, should have worked");
            }
            String orderId2 = "orderid2";
            eventService.reserveSeats(sessionToken, orderId2, testEvent.eventId(), seatIds);

            assertThrows(Exception.class, () -> activeOrderService.addSeatsToActiveOrder(session, order.getOrderId(), seatIds));
            ActiveOrderDTO orderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            for (String seatId : seatIds) {
                assertFalse(orderDTO.getSeatIds().contains(seatId));
            }
            assertTrue(eventService.checkSeatsReserved(sessionToken, orderId2, order.getEventId(), seatIds).isEmpty());
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenTwoUsersTryingToReserveSameSeat_WhenAddSeatsToActiveOrder_ThenExactlyOneSucceedsAndOneFails() throws InterruptedException {
        String token1 = authenticationService.login(USER1_ID);
        SessionToken session1 = new SessionToken(token1, 1000);
        ActiveOrderItem order1 = activeOrderService.createPendingOrder(session1, USER1_ID, testEvent.eventId());

        String user2 = "456";
        String token2 = authenticationService.login(user2);
        SessionToken session2 = new SessionToken(token2, 1000);
        ActiveOrderItem order2 = activeOrderService.createPendingOrder(session2, user2, testEvent.eventId());

        String contestedSeatId = seatIds.getFirst();
        List<String> seatToReserve = List.of(contestedSeatId);

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch endLatch = new java.util.concurrent.CountDownLatch(2);

        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failureCount = new java.util.concurrent.atomic.AtomicInteger(0);

        executor.submit(() -> {
            try {
                startLatch.await();
                activeOrderService.addSeatsToActiveOrder(session1, order1.getOrderId(), seatToReserve);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                activeOrderService.addSeatsToActiveOrder(session2, order2.getOrderId(), seatToReserve);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();

        boolean completedTimely = endLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue(completedTimely, "The concurrency test timed out! Possible deadlock or infrastructure slowdown.");

        assertEquals(1, successCount.get(), "Exactly one thread must successfully reserve the seat");
        assertEquals(1, failureCount.get(), "Exactly one thread must fail due to race condition");

        executor.shutdown();
    }

    // UC II.2.7
    @Test
    public void GivenValidStandingArea_WhenAddStandingAreaToActiveOrder_ThenAddAreaToOrder() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (order == null) {
                fail("couldnt create active order, should have worked");
            }

            String validAreaId = standingAreas.getFirst();
            int inventoryBefore = seatingMap.getArea(validAreaId).getAvalibleSeatNumber();
            int quantity = 2;

            assertDoesNotThrow(() -> activeOrderService.addStandingAreaToActiveOrder(session, order.getOrderId(), validAreaId, quantity));

            ActiveOrderDTO updatedOrder = activeOrderService.getActiveOrderInfo(session, order.getOrderId());

            assertTrue(updatedOrder.getStandingAreaQuantities().containsKey(validAreaId));
            assertEquals(quantity, updatedOrder.getStandingAreaQuantities().get(validAreaId));
            assertEquals(inventoryBefore - quantity, seatingMap.getArea(validAreaId).getAvalibleSeatNumber());
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenStandingAreaThatDoesntExist_WhenAddStandingAreaToActiveOrder_ThenAreaNotAddedToOrder() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());

            if (order == null) {
                fail("couldnt create active order, should have worked");
            }

            String nonExistingAreaId = "NonExistentAreaId";
            int quantity = 2;

            assertThrows(Exception.class, () ->
                    activeOrderService.addStandingAreaToActiveOrder(session, order.getOrderId(), nonExistingAreaId, quantity)
            );

            ActiveOrderDTO orderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            assertFalse(orderDTO.getStandingAreaQuantities().containsKey(nonExistingAreaId));
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenStandingAreaInsufficientCapacity_WhenAddStandingAreaToActiveOrder_ThenOrderDoesntHaveTheArea() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());

            if (order == null) {
                fail("couldnt create active order, should have worked");
            }

            String validAreaId = standingAreas.getFirst();
            int excessiveQuantity = 999999;

            assertThrows(Exception.class, () ->
                    activeOrderService.addStandingAreaToActiveOrder(session, order.getOrderId(), validAreaId, excessiveQuantity)
            );

            ActiveOrderDTO orderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            assertFalse(orderDTO.getStandingAreaQuantities().containsKey(validAreaId));
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenOneTicketLeftInStandingArea_WhenConcurrentRequests_ThenExactlyOneSucceedsAndOneFails() throws InterruptedException {
        String standingAreaId = standingAreas.getFirst();

        String dummyUserId = "dummyUser";
        String dummyToken = authenticationService.login(dummyUserId);
        SessionToken dummySession = new SessionToken(dummyToken, 1000);
        ActiveOrderItem dummyOrder = activeOrderService.createPendingOrder(dummySession, dummyUserId, testEvent.eventId());

        activeOrderService.addStandingAreaToActiveOrder(dummySession, dummyOrder.getOrderId(), standingAreaId, 19);

        String token1 = authenticationService.login(USER1_ID);
        SessionToken session1 = new SessionToken(token1, 1000);
        ActiveOrderItem order1 = activeOrderService.createPendingOrder(session1, USER1_ID, testEvent.eventId());

        String user2 = "456";
        String token2 = authenticationService.login(user2);
        SessionToken session2 = new SessionToken(token2, 1000);
        ActiveOrderItem order2 = activeOrderService.createPendingOrder(session2, user2, testEvent.eventId());

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch endLatch = new java.util.concurrent.CountDownLatch(2);

        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failureCount = new java.util.concurrent.atomic.AtomicInteger(0);

        executor.submit(() -> {
            try {
                startLatch.await();
                activeOrderService.addStandingAreaToActiveOrder(session1, order1.getOrderId(), standingAreaId, 1);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                activeOrderService.addStandingAreaToActiveOrder(session2, order2.getOrderId(), standingAreaId, 1);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();

        boolean completedTimely = endLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue(completedTimely, "The standing area concurrency test timed out!");

        assertEquals(1, successCount.get(), "Exactly one user should successfully get the last standing ticket");
        assertEquals(1, failureCount.get(), "Exactly one user should fail due to insufficient capacity");

        executor.shutdown();
    }

    // UC II.2.9
    @Test
    public void GivenValidOrderId_WhenGetActiveOrderInfo_ThenReturnDTOofOrder() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (order == null) {
                fail("couldnt create active order, should have worked");
            }
            ActiveOrderDTO orderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            assertNotNull(orderDTO);
            assertEquals(orderDTO.getOrderId(), order.getOrderId());
            assertEquals(orderDTO.getEventId(), order.getEventId());
            assertEquals(orderDTO.getUserId(), order.getUserId());
            assertEquals(orderDTO.getSeatIds(), order.getSeatIds());
            assertEquals(orderDTO.getStandingAreaQuantities(), order.getStandingAreaQuantities());
            assertEquals(orderDTO.getCreatedAt().getTime(), order.getCreatedAt().getTime());
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenNonExistentOrderId_WhenGetActiveOrderInfo_ThenThrowException() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (order == null) {
                fail("couldnt create active order, should have worked");
            }
            String nonExistentOrder = "nonExistentOrder";
            assertThrows(Exception.class, () -> activeOrderService.getActiveOrderInfo(session, nonExistentOrder));
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    // UC II.2.10
    @Test
    public void GivenEmptySeatIds_WhenUpdateActiveOrder_ThenUpdateOrderSuccesfully() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (order == null) {
                fail("couldnt create active order, should have worked");
            }
            activeOrderService.addSeatsToActiveOrder(session, order.getOrderId(), seatIds);
            ActiveOrderDTO orderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            orderDTO.setSeatIds(List.of());
            activeOrderService.updateActiveOrder(session, orderDTO);
            ActiveOrderDTO updateOrderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            assertTrue(updateOrderDTO.getSeatIds().isEmpty());
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenValidNonEmptySeatIds_WhenUpdateActiveOrder_ThenUpdateOrderSuccesfully() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (order == null) {
                fail("couldnt create active order, should have worked");
            }
            activeOrderService.addSeatsToActiveOrder(session, order.getOrderId(), seatIds);
            ActiveOrderDTO orderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            List<String> newSeats = List.of(seatIds.getFirst());
            orderDTO.setSeatIds(newSeats);
            activeOrderService.updateActiveOrder(session, orderDTO);
            ActiveOrderDTO updateOrderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            assertTrue(updateOrderDTO.getSeatIds().containsAll(newSeats));
            seatIds.removeAll(newSeats);
            for (String seatID : seatIds) {
                assertFalse(updateOrderDTO.getSeatIds().contains(seatID));
            }
            assertTrue(eventService.checkSeatsReserved(sessionToken, order.getOrderId(), order.getEventId(), newSeats).isEmpty());
            List<String> releasedSeats = new ArrayList<>(orderDTO.getSeatIds());
            releasedSeats.removeAll(newSeats);
            assertEquals(eventService.checkSeatsReserved(sessionToken, order.getOrderId(), order.getEventId(), releasedSeats), releasedSeats);
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenInvalidDetails_WhenUpdateActiveOrder_ThenThrowExceptionAndDontChangeOrder() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (order == null) {
                fail("couldnt create active order, should have worked");
            }
            activeOrderService.addSeatsToActiveOrder(session, order.getOrderId(), seatIds);
            ActiveOrderDTO orderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            List<String> oldSeats = orderDTO.getSeatIds();
            List<String> newSeats = List.of("nonExistentSeat");
            orderDTO.setSeatIds(newSeats);

            assertThrows(Exception.class, () -> activeOrderService.updateActiveOrder(session, orderDTO));
            ActiveOrderDTO updatedOrderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            assertEquals(updatedOrderDTO.getSeatIds(), oldSeats);
            assertTrue(eventService.checkSeatsReserved(sessionToken, order.getOrderId(), order.getEventId(), seatIds).isEmpty());
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenEmptyStandingAreaQuantities_WhenUpdateActiveOrder_ThenUpdateOrderSuccessfully() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (order == null) {
                fail("couldnt create active order, should have worked");
            }

            String validAreaId = standingAreas.getFirst();
            int quantity = 1;
            activeOrderService.addStandingAreaToActiveOrder(session, order.getOrderId(), validAreaId, quantity);

            ActiveOrderDTO orderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());

            java.util.HashMap<String, Integer> emptyQuantities = new java.util.HashMap<>();
            orderDTO.setStandingAreaQuantities(emptyQuantities);

            int inventoryBefore = seatingMap.getArea(validAreaId).getAvalibleSeatNumber();
            activeOrderService.updateActiveOrder(session, orderDTO);
            ActiveOrderDTO updateOrderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            assertTrue(updateOrderDTO.getStandingAreaQuantities().isEmpty());
            assertEquals(inventoryBefore + quantity, seatingMap.getArea(validAreaId).getAvalibleSeatNumber());
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenValidNonEmptyStandingAreaQuantities_WhenUpdateActiveOrder_ThenUpdateOrderSuccessfully() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (order == null) {
                fail("couldnt create active order, should have worked");
            }

            String validAreaId = standingAreas.getFirst();
            int quantity = 5;
            activeOrderService.addStandingAreaToActiveOrder(session, order.getOrderId(), validAreaId, quantity);

            ActiveOrderDTO orderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            int newQuantity = 2;

            java.util.HashMap<String, Integer> updatedQuantities = new java.util.HashMap<>();
            updatedQuantities.put(validAreaId, newQuantity);
            orderDTO.setStandingAreaQuantities(updatedQuantities);
            int inventoryBefore = seatingMap.getArea(validAreaId).getAvalibleSeatNumber();

            activeOrderService.updateActiveOrder(session, orderDTO);
            ActiveOrderDTO updateOrderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            int inventoryAfter = seatingMap.getArea(validAreaId).getAvalibleSeatNumber();
            assertTrue(updateOrderDTO.getStandingAreaQuantities().containsKey(validAreaId));
            assertEquals(newQuantity, updateOrderDTO.getStandingAreaQuantities().get(validAreaId));
            assertEquals(inventoryBefore + quantity - newQuantity, inventoryAfter);
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenInvalidStandingAreaDetails_WhenUpdateActiveOrder_ThenThrowExceptionAndDontChangeOrder() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (order == null) {
                fail("couldnt create active order, should have worked");
            }

            String validAreaId = standingAreas.getFirst();
            int originalQuantity = 3;
            activeOrderService.addStandingAreaToActiveOrder(session, order.getOrderId(), validAreaId, originalQuantity);

            ActiveOrderDTO orderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            String nonExistentArea = "nonExistentArea";

            java.util.HashMap<String, Integer> invalidQuantities = new java.util.HashMap<>();
            invalidQuantities.put(nonExistentArea, 2);
            orderDTO.setStandingAreaQuantities(invalidQuantities);

            assertThrows(Exception.class, () -> activeOrderService.updateActiveOrder(session, orderDTO));

            ActiveOrderDTO updatedOrderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            assertTrue(updatedOrderDTO.getStandingAreaQuantities().containsKey(validAreaId));
            assertEquals(originalQuantity, updatedOrderDTO.getStandingAreaQuantities().get(validAreaId));
            assertFalse(updatedOrderDTO.getStandingAreaQuantities().containsKey(nonExistentArea));
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    // UC II.2.11
    @Test
    public void GivenValidActiveOrder_WhenCompleteOrder_ThenActiveOrderIsDeleted() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem orderItem = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (orderItem == null) {
                fail("couldnt create active order, should have worked");
            }
            activeOrderService.addSeatsToActiveOrder(session, orderItem.getOrderId(), seatIds);
            ActiveOrderDTO order = activeOrderService.getActiveOrderInfo(session, orderItem.getOrderId());

            double amountToPay = 100;
            when(paymentGatewayMock.pay(any())).thenReturn(50000);
            when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(List.of(new BarcodeDTO("barcode1")));

            assertDoesNotThrow(() -> activeOrderService.completeOrder(paymentGatewayMock, session, paymentDetailsFor(amountToPay), order.getOrderId(), null));
            assertThrows(Exception.class, () -> activeOrderService.getActiveOrderInfo(session, order.getOrderId()));
            assertTrue(activeOrderRepo.findById(order.getOrderId()).isEmpty());

            ActiveOrderItem order2 = assertDoesNotThrow(() -> activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId()));
            assertNotNull(order2);
            assertTrue(eventService.checkSeatsReserved(sessionToken, order.getOrderId(), order.getEventId(), seatIds).isEmpty());
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenValidActiveOrder_WhenCompleteOrder_ThenHistoryOrderIsMade() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem orderItem = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (orderItem == null) {
                fail("couldnt create active order, should have worked");
            }
            activeOrderService.addSeatsToActiveOrder(session, orderItem.getOrderId(), seatIds);
            ActiveOrderDTO order = activeOrderService.getActiveOrderInfo(session, orderItem.getOrderId());
            double amountToPay = 100;
            when(paymentGatewayMock.pay(any())).thenReturn(50000);
            when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(List.of(new BarcodeDTO("barcode1")));

            assertDoesNotThrow(() -> activeOrderService.completeOrder(paymentGatewayMock, session, paymentDetailsFor(amountToPay), order.getOrderId(),null));
            HistoryOrderItem historyOrderItem = assertDoesNotThrow(() -> historyOrderRepo.findByOrderId(order.getOrderId()));
            assertNotNull(historyOrderItem);
            HistoryOrderDTO historyOrderDTO = historyOrderItem.makeDTO();

            assertEquals(historyOrderDTO.getEventId(), order.getEventId());
            assertEquals(historyOrderDTO.getUserId(), order.getUserId());
            assertEquals(historyOrderDTO.getCompanyId(), companyId);
            assertEquals(historyOrderDTO.getPrice(), amountToPay);
            assertTrue(sameElements(historyOrderDTO.getSeatIds(), order.getSeatIds()));
//            assertEquals(historyOrderDTO.getSeatIds(), order.getSeatIds());
            assertEquals(historyOrderDTO.getStandingAreaQuantities(), order.getStandingAreaQuantities());
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenExpiredOrder_WhenCompleteOrder_ThenDeleteOrderAndThrowException() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem orderItem = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (orderItem == null) {
                fail("couldnt create active order, should have worked");
            }
            activeOrderService.addSeatsToActiveOrder(session, orderItem.getOrderId(), seatIds);
            ActiveOrderDTO order = activeOrderService.getActiveOrderInfo(session, orderItem.getOrderId());

            double amountToPay = 100;
            order.setCreatedAt(java.sql.Timestamp.valueOf("1977-10-10 00:00:00"));
            activeOrderRepo.update(new ActiveOrderItem(order));
            assertThrows(Exception.class, () -> activeOrderService.completeOrder(paymentGatewayMock, session, paymentDetailsFor(amountToPay), order.getOrderId(), null));
            assertTrue(activeOrderRepo.findById(order.getOrderId()).isEmpty());
            assertEquals(eventService.checkSeatsReserved(sessionToken, order.getOrderId(), order.getEventId(), seatIds), seatIds);
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenPaymentFailed_WhenCompleteOrder_ThenDeleteOrderAndThrowException() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem orderItem = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (orderItem == null) {
                fail("couldnt create active order, should have worked");
            }
            activeOrderService.addSeatsToActiveOrder(session, orderItem.getOrderId(), seatIds);
            ActiveOrderDTO order = activeOrderService.getActiveOrderInfo(session, orderItem.getOrderId());

            double amountToPay = 100;
            when(paymentGatewayMock.pay(any())).thenReturn(-1);

            assertThrows(Exception.class, () -> activeOrderService.completeOrder(paymentGatewayMock, session, paymentDetailsFor(amountToPay), order.getOrderId(), null));
            assertTrue(activeOrderRepo.findById(order.getOrderId()).isEmpty());
            assertEquals(eventService.checkSeatsReserved(sessionToken, order.getOrderId(), order.getEventId(), seatIds), seatIds);
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenBarcodeIssueFailed_WhenCompleteOrder_ThenDeleteOrderAndThrowException() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem orderItem = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (orderItem == null) {
                fail("couldnt create active order, should have worked");
            }
            activeOrderService.addSeatsToActiveOrder(session, orderItem.getOrderId(), seatIds);
            ActiveOrderDTO order = activeOrderService.getActiveOrderInfo(session, orderItem.getOrderId());

            double amountToPay = 100;
            when(paymentGatewayMock.pay(any())).thenReturn(50000);
            when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(null);

            assertThrows(Exception.class, () -> activeOrderService.completeOrder(paymentGatewayMock, session, paymentDetailsFor(amountToPay), order.getOrderId(),null));
            assertTrue(activeOrderRepo.findById(order.getOrderId()).isEmpty());
            assertEquals(eventService.checkSeatsReserved(sessionToken, order.getOrderId(), order.getEventId(), seatIds), seatIds);
        } catch (Exception e) {
            fail("got exception : " + e.getMessage() + '\n' + java.util.Arrays.toString(e.getStackTrace()));
        }
    }

    @Test
    public void GivenOrderIsntFitWithPurchasePolicy_WhenCompleteOrder_ThenOrderIsntPayedFor() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem orderItem = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (orderItem == null) {
                fail("couldnt create active order, should have worked");
            }
            activeOrderService.addSeatsToActiveOrder(session, orderItem.getOrderId(), seatIds);
            ActiveOrderDTO order = activeOrderService.getActiveOrderInfo(session, orderItem.getOrderId());

            double amountToPay = 100;
            PurchasePolicyDTO newPolicy = new PurchasePolicyDTO(10, eventCapacity, false, 0, 60, true, false);
            eventService.editEventPurchasePolicy(sessionToken, testEvent.eventId(), newPolicy);

            assertThrows(Exception.class, () -> activeOrderService.completeOrder(paymentGatewayMock, session, paymentDetailsFor(amountToPay), order.getOrderId(),null));
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    // UC II.2.12
    @Test
    public void GivenValidOrder_WhenCancelActiveOrder_ThenOrderIsDeleted() {
        String sessionToken = authenticationService.login(USER1_ID);
        SessionToken session = new SessionToken(sessionToken, 1000);
        ActiveOrderItem orderItem = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
        if (orderItem == null) {
            fail("couldnt create active order, should have worked");
        }
        assertDoesNotThrow(() -> activeOrderService.cancelActiveOrder(session, USER1_ID, orderItem.getOrderId()));
        assertThrows(Exception.class, () -> activeOrderService.getActiveOrderInfo(session, orderItem.getOrderId()));
        assertTrue(activeOrderRepo.findById(orderItem.getOrderId()).isEmpty());
    }

    @Test
    public void GivenNonExistentOrder_WhenCancelActiveOrder_ThenThrowException() {
        try {
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);

            String nonExistentOrderId = "fake-order-id-123";
            assertThrows(Exception.class, () ->
                    activeOrderService.cancelActiveOrder(session, USER1_ID, nonExistentOrderId)
            );
        } catch (Exception e) {
            fail("got exception : " + e.getMessage());
        }
    }

    private boolean sameElements(List<String> l1, List<String> l2){
        boolean same = true;
        for(String element : l1){
            if(!l2.contains(element)){
                same = false;
                break;
            }
        }
        if(!same){
            return false;
        }
        else{
            for(String element : l2){
                if(!l1.contains(element)){
                    same = false;
                    break;
                }
            }
            return same;
        }
    }




    // UC II.2.11 + II.2.12 — Concurrency: processing flag prevents simultaneous complete and cancel
    @Test
    public void GivenSameOrder_WhenCompleteAndCancelConcurrently_ThenOnlyOneSucceedsAndOrderRemoved() throws InterruptedException {
        registeredUsers.add(USER1_ID);
        // 1. Setup: Register user and create an order using the existing environment
        String sessionToken = authenticationService.login(USER1_ID);
        SessionToken session = new SessionToken(sessionToken, 1000);
        ActiveOrderItem orderItem = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
        String liveOrderId = orderItem.getOrderId();
        try {

            // 2. Setup concurrency tools
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(2);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // Mock dependencies for the completion path
            lenient().when(paymentGatewayMock.pay(any())).thenReturn(50000);
            lenient().when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(List.of(new BarcodeDTO("barcode1")));

            // 3. Thread 1: Try to complete
            new Thread(() -> {
                try {
                    startLatch.await();
                    activeOrderService.completeOrder(paymentGatewayMock, session, paymentDetailsFor(100.0), liveOrderId, null);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();

            // 4. Thread 2: Try to cancel
            new Thread(() -> {
                try {
                    startLatch.await();
                    activeOrderService.cancelActiveOrder(session, USER1_ID, liveOrderId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();

            // 5. Execute
            startLatch.countDown();
            boolean completedTimely = doneLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);

            // 6. Assertions
            assertTrue(completedTimely, "The concurrency test timed out!");
            assertEquals(1, successCount.get(), "Exactly one operation should succeed");
            assertEquals(1, failCount.get(), "Exactly one operation should fail");
            assertTrue(activeOrderRepo.findById(liveOrderId).isEmpty(), "The order should be removed from the repo regardless of which operation won");
        } finally {
            activeOrderRepo.deleteAll();
        }
    }
}