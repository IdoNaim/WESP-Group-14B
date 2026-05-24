package com.ticketpurchasingsystem.project.acceptance.activeorder;

import com.ticketpurchasingsystem.project.application.*;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderHandler;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderListener;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import com.ticketpurchasingsystem.project.domain.event.*;
import com.ticketpurchasingsystem.project.infrastructure.ActiveOrderMemRepo;
import com.ticketpurchasingsystem.project.infrastructure.BarCodeGateway;
import com.ticketpurchasingsystem.project.infrastructure.EventRepo;
import com.ticketpurchasingsystem.project.infrastructure.HistoryOrderRepo;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

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

    ;
    //external systems
    @Mock
    private IPaymentGateway paymentGatewayMock;
    @Mock
    private IBarCodeGateway barcodeGatewayMock;

    private static final String VALID_TOKEN = "accept-token";
    private static final SessionToken VALID_SESSION = new SessionToken(VALID_TOKEN, 9999999999L);
    private static final String USER1_ID = "123";
    private static final double AMOUNT = 150.0;
    private final Set<String> registeredUsers = new HashSet<>();
    private int companyId = 10;
    private String eventName = "EventA";
    private Integer eventCapacity = 50;
    private PurchasePolicyDTO purchasePolicyDTO = new PurchasePolicyDTO(0, eventCapacity,10, 60, true);
    private List<DiscountDTO> discounts = new ArrayList<>();
    private LocalDateTime eventDate = LocalDateTime.of(LocalDate.now().plusYears(1), LocalTime.now());



    @BeforeEach
    public void setup() {
        registeredUsers.clear();

        eventRepo = new EventRepo();
        eventListener = new EventAggregateListener(eventRepo, eventService);
        eventPublisher = new EventAggregatePublisher(event -> {});          /// not important for these acceptance tests
        eventService = new EventService(eventRepo, eventPublisher, eventListener);


        historyOrderRepo = new HistoryOrderRepo();
        HistoryOrderHandler historyOrderHandler = new HistoryOrderHandler();
        ProductionService productionService = new ProductionService(authenticationService, null, null, null);
        historyOrderService = new HistoryOrderService(historyOrderRepo, historyOrderHandler, authenticationService, productionService);
        historyOrderListener = new HistoryOrderListener(historyOrderRepo, historyOrderService);

        sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        authenticationService = new AuthenticationService(domainAuthService, sessionRepo);
        ReflectionTestUtils.setField(domainAuthService, "secret", TEST_SECRET);
        domainAuthService.init();

        activeOrderHandler = new ActiveOrderHandler();
        activeOrderRepo = new ActiveOrderMemRepo();
        activeOrderListener = new ActiveOrderListener(activeOrderRepo);
        activeOrderPublisher = new ActiveOrderPublisher(event -> {
            if (event instanceof IsValidEventIDEvent e) {
                eventListener.onIsValidEventIDEvent(e);
            }
            else if (event instanceof GetCompanyIdEvent e) {
                eventListener.onGetCompanyIdEvent(e);
            }
            else if (event instanceof IsUpToPolicyEvent e) {
                eventListener.onIsUpToPolicyEvent(e);
            }
            else if (event instanceof CompletedOrderEvent e) {
                historyOrderListener.onCompletedOrder(e);
            }
            else if (event instanceof SeatReleaseEvent e) {
                eventListener.onSeatReleaseEvent(e);
            }
            else if (event instanceof SeatReservationEvent e) {
                eventListener.onSeatReservationEvent(e);
            }
            else if (event instanceof StandingAreaReleaseEvent e) {
                eventListener.onStandingAreaReleaseEvent(e);
            }
            else if (event instanceof StandingAreaReservationEvent e) {
                eventListener.onStandingAreaReservationEvent(e);
            }
        });
        activeOrderService = new ActiveOrderService(activeOrderListener,activeOrderPublisher,activeOrderRepo, activeOrderHandler, authenticationService, barcodeGatewayMock);
        EventDTO e = new EventDTO(null,
                companyId,
                eventName,
                eventCapacity,
                eventDate,
                true);
        testEvent = eventService.createEvent(e, purchasePolicyDTO, discounts);
        this.seatingMap = eventService.configureSeatingMap(List.of(new SeatingAreaConfig(1, 1, 10)), List.of(new StandingAreaConfig(20, 10)));
        seatIds = new ArrayList<>(seatingMap.getSeats());
        standingAreas = new ArrayList<>(seatingMap.getStandingAreas());
        eventService.editEventSeatingMap(testEvent.eventId(), seatingMap);

    }
    @Test
    public void checkSetup(){
        assertTrue(true);
        System.out.println("setup complete!");
    }

    //UC II.2.5
    @Test
    public void GivenValidEventId_WhenCreateOrder_ThenCreateActiveOrderSuccessfully(){
        registeredUsers.add(USER1_ID);
        String sessionToken = authenticationService.login(USER1_ID);

        ActiveOrderItem order = activeOrderService.createPendingOrder(new SessionToken(sessionToken, 1000),USER1_ID ,testEvent.eventId());
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

        ActiveOrderItem order = activeOrderService.createPendingOrder(session,USER1_ID ,testEvent.eventId());
        if(order == null){
            fail("couldnt create first active order, should have worked");
        }
        assertThrows(Exception.class, () ->activeOrderService.createPendingOrder(session, USER1_ID,testEvent.eventId()));
    }
    @Test
    public void GivenEventDoesntExist_WhenCreateOrder_ThenThrowException(){
        registeredUsers.add(USER1_ID);
        String sessionToken = authenticationService.login(USER1_ID);
        SessionToken session = new SessionToken(sessionToken, 1000);

        assertThrows(Exception.class, () ->activeOrderService.createPendingOrder(session, USER1_ID, "nonExistentId"));
    }

    //UC II.2.6
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
            assertTrue(updatedOrder.getSeatIds().equals(seatIds));
        }catch (Exception e){
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
            //TODO: add a check that each seat is reserved for orderId
        }catch (Exception e){
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
            activeOrderService.addSeatsToActiveOrder(session,order.getOrderId(),List.of(nonExistingSeatId));
            ActiveOrderDTO orderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            assertFalse(orderDTO.getSeatIds().contains(nonExistingSeatId));
        }catch (Exception e){
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
        }catch (Exception e){
            fail("got exception : "+ e.getMessage());
        }
    }

    //UC II.2.7
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
            int quantity = 2;

            assertDoesNotThrow(() -> activeOrderService.addStandingAreaToActiveOrder(session, order.getOrderId(), validAreaId, quantity));

            ActiveOrderDTO updatedOrder = activeOrderService.getActiveOrderInfo(session, order.getOrderId());

            assertTrue(updatedOrder.getStandingAreaQuantities().containsKey(validAreaId));
            assertEquals(quantity, updatedOrder.getStandingAreaQuantities().get(validAreaId));
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

            // Expecting an exception because the area cannot be reserved
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
            int excessiveQuantity = 999999; // A quantity guaranteed to exceed area capacity

            // Expecting an exception due to insufficient capacity
            assertThrows(Exception.class, () ->
                    activeOrderService.addStandingAreaToActiveOrder(session, order.getOrderId(), validAreaId, excessiveQuantity)
            );

            ActiveOrderDTO orderDTO = activeOrderService.getActiveOrderInfo(session, order.getOrderId());
            assertFalse(orderDTO.getStandingAreaQuantities().containsKey(validAreaId));
        } catch (Exception e){
            fail("got exception : "+ e.getMessage());
        }
    }
    //UC II.2.9
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
            assertEquals(orderDTO.getSeatIds(),order.getSeatIds());
            assertEquals(orderDTO.getStandingAreaQuantities(), order.getStandingAreaQuantities());
            assertTrue(orderDTO.getCreatedAt().getTime() == order.getCreatedAt().getTime());

        }catch (Exception e) {
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

        }catch (Exception e){
            fail("got exception : " + e.getMessage());
        }
    }
    //UC II.2.10
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
        }catch (Exception e){
            fail("got exception : " + e.getMessage());
        }
    }
    @Test
    public
}
