package com.ticketpurchasingsystem.project.acceptance.activeorder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ticketpurchasingsystem.project.application.ActiveOrderService;
import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.application.HistoryOrderService;
import com.ticketpurchasingsystem.project.application.IActiveOrderService;
import com.ticketpurchasingsystem.project.application.IBarCodeGateway;
import com.ticketpurchasingsystem.project.application.IEventService;
import com.ticketpurchasingsystem.project.application.IHistoryOrderService;
import com.ticketpurchasingsystem.project.application.IPaymentGateway;
import com.ticketpurchasingsystem.project.application.PaymentDetails;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.CompletedOrderEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.GetCompanyIdEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.IsUpToPolicyEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.IsValidEventIDEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.SeatReleaseEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.SeatReservationEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.StandingAreaReleaseEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.StandingAreaReservationEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderHandler;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderListener;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderPublisher;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.IActiveOrderRepo;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderHandler;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderListener;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import com.ticketpurchasingsystem.project.domain.event.EventAggregateListener;
import com.ticketpurchasingsystem.project.domain.event.EventAggregatePublisher;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingAreaConfig;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingMap;
import com.ticketpurchasingsystem.project.domain.event.Maps.StandingAreaConfig;
import com.ticketpurchasingsystem.project.infrastructure.ActiveOrderMemRepo;
import com.ticketpurchasingsystem.project.infrastructure.EventRepo;
import com.ticketpurchasingsystem.project.infrastructure.HistoryOrderRepo;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import com.ticketpurchasingsystem.project.infrastructure.ProdRepo;

@ExtendWith(MockitoExtension.class)
public class ActiveOrderAcceptanceTests {
    private IActiveOrderService activeOrderService;
    private IActiveOrderRepo activeOrderRepo;
    private ActiveOrderHandler activeOrderHandler;
    private ActiveOrderPublisher activeOrderPublisher;
    private ActiveOrderListener activeOrderListener;

    private ISessionRepo sessionRepo;
    private AuthenticationService authenticationService;
    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";

    private IEventService eventService;
    private EventAggregateListener eventListener;
    private IEventRepo eventRepo;
    private EventAggregatePublisher eventPublisher;
    private EventDTO testEvent;
    private SeatingMap seatingMap;
    private List<String> seatIds;
    private List<String> standingAreas;

    private HistoryOrderListener historyOrderListener;
    private IHistoryOrderRepo historyOrderRepo;
    private IHistoryOrderService historyOrderService;

    @Mock
    private IPaymentGateway paymentGatewayMock;
    @Mock
    private IBarCodeGateway barcodeGatewayMock;

    private static final String VALID_TOKEN = "accept-token";
    private static final SessionToken VALID_SESSION = new SessionToken(VALID_TOKEN, 9999999999L);
    private static final String USER1_ID = "123";
    private static final String USER2_ID = "456";
    private static final double AMOUNT = 150.0;
    private final Set<String> registeredUsers = new HashSet<>();
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

        sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        authenticationService = new AuthenticationService(domainAuthService, sessionRepo);
        ReflectionTestUtils.setField(domainAuthService, "secret", TEST_SECRET);
        domainAuthService.init();

        eventRepo = new EventRepo();
        eventPublisher = new EventAggregatePublisher(event -> {});
        eventService = new EventService(eventRepo, eventPublisher, authenticationService);
        eventListener = new EventAggregateListener(eventRepo, eventService);

        historyOrderRepo = new HistoryOrderRepo();
        HistoryOrderHandler historyOrderHandler = new HistoryOrderHandler();
        ProductionHandler prodHandler = new ProductionHandler();
        ProdRepo prodRepo = new ProdRepo();
        ProductionService productionService = new ProductionService(authenticationService, prodHandler, prodRepo, null);
        historyOrderService = new HistoryOrderService(historyOrderRepo, historyOrderHandler, authenticationService, productionService);
        historyOrderListener = new HistoryOrderListener(historyOrderRepo, historyOrderService);

        activeOrderHandler = new ActiveOrderHandler();
        activeOrderRepo = new ActiveOrderMemRepo();
        activeOrderListener = new ActiveOrderListener(activeOrderRepo);
        activeOrderPublisher = new ActiveOrderPublisher(event -> {
            if (event instanceof IsValidEventIDEvent e) {
                eventListener.onIsValidEventIDEvent(e);
            } else if (event instanceof GetCompanyIdEvent e) {
                eventListener.onGetCompanyIdEvent(e);
            } else if (event instanceof IsUpToPolicyEvent e) {
                eventListener.onIsUpToPolicyEvent(e);
            } else if (event instanceof CompletedOrderEvent e) {
                historyOrderListener.onCompletedOrder(e);
            } else if (event instanceof SeatReleaseEvent e) {
                eventListener.onSeatReleaseEvent(e);
            } else if (event instanceof SeatReservationEvent e) {
                eventListener.onSeatReservationEvent(e);
            } else if (event instanceof StandingAreaReleaseEvent e) {
                eventListener.onStandingAreaReleaseEvent(e);
            } else if (event instanceof StandingAreaReservationEvent e) {
                eventListener.onStandingAreaReservationEvent(e);
            }
        });
        activeOrderService = new ActiveOrderService(activeOrderListener, activeOrderPublisher, activeOrderRepo, activeOrderHandler, authenticationService, barcodeGatewayMock);

        // ✅ FIXED: Expanded to 10 parameters to match your updated EventDTO record
        EventDTO e = new EventDTO(
                null,
                companyId,
                eventName,
                eventCapacity,
                eventDate,
                true,
                eventLocation,
                null,  // imageUrl
                null,  // minZonePrice
                null   // maxZonePrice
        );

        registeredUsers.add(USER2_ID);
        String sessionToken = authenticationService.login(USER2_ID);

        eventService.createEvent(sessionToken, e, purchasePolicyDTO, discounts);
        List<EventDTO> events = eventService.searchEventsByCompany(sessionToken, companyId);
        testEvent = events.getFirst();
        this.seatingMap = eventService.configureSeatingMap(sessionToken, List.of(new SeatingAreaConfig(1, 2, 10)), List.of(new StandingAreaConfig(20, 10)));
        seatIds = new ArrayList<>(seatingMap.getSeatIds());
        standingAreas = new ArrayList<>(seatingMap.getAreaIds());
        eventService.editEventSeatingMap(sessionToken, testEvent.eventId(), seatingMap);
    }

    @Test
    public void givenTestEnvironmentSetup_whenCheckingSetup_thenAssertionSucceeds(){
        assertTrue(true);
        System.out.println("setup complete!");
    }

    @Test
    public void GivenValidEventId_WhenCreateOrder_ThenCreateActiveOrderSuccessfully(){
        registeredUsers.add(USER1_ID);
        String sessionToken = authenticationService.login(USER1_ID);

        ActiveOrderItem order = activeOrderService.createPendingOrder(new SessionToken(sessionToken, 1000), USER1_ID, testEvent.eventId());
        assertNotNull(order);
        assertEquals(order.getEventId(), testEvent.eventId());
        assertEquals(order.getUserId(), USER1_ID);
        assertNotNull(order.getCreatedAt());
    }

    @Test
    public void GivenUserAlreadyHasActiveOrder_WhenCreateOrder_ThenThrowException(){
        registeredUsers.add(USER1_ID);
        String sessionToken = authenticationService.login(USER1_ID);
        SessionToken session = new SessionToken(sessionToken, 1000);

        ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
        if(order == null){
            fail("couldnt create first active order, should have worked");
        }
        assertThrows(Exception.class, () -> activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId()));
    }

    @Test
    public void GivenEventDoesntExist_WhenCreateOrder_ThenThrowException(){
        registeredUsers.add(USER1_ID);
        String sessionToken = authenticationService.login(USER1_ID);
        SessionToken session = new SessionToken(sessionToken, 1000);

        assertThrows(Exception.class, () -> activeOrderService.createPendingOrder(session, USER1_ID, "nonExistentId"));
    }

    @Test
    public void GivenValidSeatIds_WhenAddSeatsToActiveOrder_ThenAddSeatsToOrder(){
        try {
            registeredUsers.add(USER1_ID);
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());

            if (order == null) {
                fail("couldnt create active order, should have worked");
            }

            assertDoesNotThrow(() -> activeOrderService.addSeatsToActiveOrder(session, order.getOrderId(), seatIds));
            ActiveOrderDTO updatedOrder = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            assertEquals(updatedOrder.getSeatIds(), seatIds);
        } catch (Exception e){
            fail("got exception : "+ e.getMessage());
        }
    }

    @Test
    public void GivenValidSeatIds_WhenAddSeatsToActiveOrder_ThenSeatsAreReservedForOrder(){
        try {
            registeredUsers.add(USER1_ID);
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());

            if (order == null) {
                fail("couldnt create active order, should have worked");
            }

            assertDoesNotThrow(() -> activeOrderService.addSeatsToActiveOrder(session, order.getOrderId(), seatIds));
            assertTrue(eventService.checkSeatsReserved(sessionToken, order.getOrderId(), order.getEventId(), seatIds).isEmpty());
        } catch (Exception e){
            fail("got exception : "+ e.getMessage());
        }
    }

    @Test
    public void GivenSeatsThatDontExist_WhenAddSeatsToActiveOrder_ThenSeatsNotAddedToOrder(){
        try{
            registeredUsers.add(USER1_ID);
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
        } catch (Exception e){
            fail("got exception : "+ e.getMessage());
        }
    }

    @Test
    public void GivenSeatsAlreadyReserved_WhenAddSeatsToActiveOrder_ThenOrderDoesntHaveTheSeats() {
        try {
            registeredUsers.add(USER1_ID);
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
        } catch (Exception e){
            fail("got exception : "+ e.getMessage());
        }
    }

    @Test
    public void GivenTwoUsersTryingToReserveSameSeat_WhenAddSeatsToActiveOrder_ThenExactlyOneSucceedsAndOneFails() throws InterruptedException {
        registeredUsers.add(USER1_ID);
        String token1 = authenticationService.login(USER1_ID);
        SessionToken session1 = new SessionToken(token1, 1000);
        ActiveOrderItem order1 = activeOrderService.createPendingOrder(session1, USER1_ID, testEvent.eventId());

        registeredUsers.add(USER2_ID);
        String token2 = authenticationService.login(USER2_ID);
        SessionToken session2 = new SessionToken(token2, 1000);
        ActiveOrderItem order2 = activeOrderService.createPendingOrder(session2, USER2_ID, testEvent.eventId());

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

    @Test
    public void GivenValidStandingArea_WhenAddStandingAreaToActiveOrder_ThenAddAreaToOrder(){
        try {
            registeredUsers.add(USER1_ID);
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
        } catch (Exception e){
            fail("got exception : "+ e.getMessage());
        }
    }

    @Test
    public void GivenStandingAreaThatDoesntExist_WhenAddStandingAreaToActiveOrder_ThenAreaNotAddedToOrder(){
        try {
            registeredUsers.add(USER1_ID);
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
        } catch (Exception e){
            fail("got exception : "+ e.getMessage());
        }
    }

    @Test
    public void GivenStandingAreaInsufficientCapacity_WhenAddStandingAreaToActiveOrder_ThenOrderDoesntHaveTheArea() {
        try {
            registeredUsers.add(USER1_ID);
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
        } catch (Exception e){
            fail("got exception : "+ e.getMessage());
        }
    }

    @Test
    public void GivenOneTicketLeftInStandingArea_WhenConcurrentRequests_ThenExactlyOneSucceedsAndOneFails() throws InterruptedException {
        String standingAreaId = standingAreas.getFirst();

        String dummyUserId = "dummyUser";
        registeredUsers.add(dummyUserId);
        String dummyToken = authenticationService.login(dummyUserId);
        SessionToken dummySession = new SessionToken(dummyToken, 1000);
        ActiveOrderItem dummyOrder = activeOrderService.createPendingOrder(dummySession, dummyUserId, testEvent.eventId());

        activeOrderService.addStandingAreaToActiveOrder(dummySession, dummyOrder.getOrderId(), standingAreaId, 19);

        registeredUsers.add(USER1_ID);
        String token1 = authenticationService.login(USER1_ID);
        SessionToken session1 = new SessionToken(token1, 1000);
        ActiveOrderItem order1 = activeOrderService.createPendingOrder(session1, USER1_ID, testEvent.eventId());

        registeredUsers.add(USER2_ID);
        String token2 = authenticationService.login(USER2_ID);
        SessionToken session2 = new SessionToken(token2, 1000);
        ActiveOrderItem order2 = activeOrderService.createPendingOrder(session2, USER2_ID, testEvent.eventId());

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

    @Test
    public void GivenValidOrderId_WhenGetActiveOrderInfo_ThenReturnDTOofOrder(){
        try {
            registeredUsers.add(USER1_ID);
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
    public void GivenNonExistentOrderId_WhenGetActiveOrderInfo_ThenThrowException(){
        try{
            registeredUsers.add(USER1_ID);
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (order == null) {
                fail("couldnt create active order, should have worked");
            }
            String nonExistentOrder = "nonExistentOrder";
            assertThrows(Exception.class, () -> activeOrderService.getActiveOrderInfo(session, nonExistentOrder));
        } catch (Exception e){
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenEmptySeatIds_WhenUpdateActiveOrder_ThenUpdateOrderSuccesfully(){
        try {
            registeredUsers.add(USER1_ID);
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
        } catch (Exception e){
            fail("got exception : " + e.getMessage());
        }
    }

    @Test
    public void GivenValidNonEmptySeatIds_WhenUpdateActiveOrder_ThenUpdateOrderSuccesfully(){
        try{
            registeredUsers.add(USER1_ID);
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
            for(String seatID : seatIds){
                assertFalse(updateOrderDTO.getSeatIds().contains(seatID));
            }
            assertTrue(eventService.checkSeatsReserved(sessionToken, order.getOrderId(), order.getEventId(), newSeats).isEmpty());
            List<String> releasedSeats = new ArrayList<>(orderDTO.getSeatIds());
            releasedSeats.removeAll(newSeats);
            assertEquals(eventService.checkSeatsReserved(sessionToken, order.getOrderId(), order.getEventId(), releasedSeats), releasedSeats);
        } catch (Exception e){
            fail("got exception : " + e.getMessage());
        }
    }

    // ✅ FIXED: Completed the truncated test method cleanly
    @Test
    public void GivenInvalidDetails_WhenUpdateActiveOrder_ThenThrowExceptionAndDontChangeOrder(){
        try {
            registeredUsers.add(USER1_ID);
            String sessionToken = authenticationService.login(USER1_ID);
            SessionToken session = new SessionToken(sessionToken, 1000);
            ActiveOrderItem order = activeOrderService.createPendingOrder(session, USER1_ID, testEvent.eventId());
            if (order == null) {
                fail("couldnt create active order, should have worked");
            }
            activeOrderService.addSeatsToActiveOrder(session, order.getOrderId(), seatIds);
            ActiveOrderDTO orderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());

            // Altering details maliciously/incompatibly to provoke an exception
            orderDTO.setSeatIds(List.of("NonExistentAndInvalidSeatID"));

            assertThrows(Exception.class, () -> activeOrderService.updateActiveOrder(session, orderDTO));

            // Verify original state remains untouched
            ActiveOrderDTO rollbackedOrderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            assertEquals(rollbackedOrderDTO.getSeatIds(), seatIds);
        } catch (Exception e){
            fail("got exception : " + e.getMessage());
        }
    }
}