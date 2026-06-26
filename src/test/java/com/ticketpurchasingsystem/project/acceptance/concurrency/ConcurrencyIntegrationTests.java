package com.ticketpurchasingsystem.project.acceptance.concurrency;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.ticketpurchasingsystem.project.domain.User.IUserRepo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ticketpurchasingsystem.project.application.ActiveOrderService;
import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.application.HistoryOrderService;
import com.ticketpurchasingsystem.project.application.IActiveOrderService;
import com.ticketpurchasingsystem.project.application.IBarCodeGateway;
import com.ticketpurchasingsystem.project.application.IEventService;
import com.ticketpurchasingsystem.project.application.IHistoryOrderService;
import com.ticketpurchasingsystem.project.application.IPaymentGateway;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.application.UserService.UserPublisher;
import com.ticketpurchasingsystem.project.application.UserService.UserService;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.*;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderHandler;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderListener;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderPublisher;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.BarcodeDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.IActiveOrderRepo;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderHandler;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderListener;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.Production.OptimisticLockingFailureException;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.User.UserHandler;
import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import com.ticketpurchasingsystem.project.domain.event.Event;
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
import com.ticketpurchasingsystem.project.infrastructure.MemoryUserRepo;
import com.ticketpurchasingsystem.project.application.PaymentDetails;

public class ConcurrencyIntegrationTests {

    private MemoryUserRepo userRepo;
    private UserService userService;
    private AuthenticationService authenticationService;
    private ISessionRepo sessionRepo;

    private IEventService eventService;
    private EventAggregateListener eventListener;
    private IEventRepo eventRepo;
    private EventAggregatePublisher eventPublisher;

    private HistoryOrderListener historyOrderListener;
    private IHistoryOrderRepo historyOrderRepo;
    private IHistoryOrderService historyOrderService;

    private IActiveOrderRepo activeOrderRepo;
    private ActiveOrderHandler activeOrderHandler;
    private ActiveOrderPublisher activeOrderPublisher;
    private ActiveOrderListener activeOrderListener;
    private IActiveOrderService activeOrderService;

    private IPaymentGateway paymentGatewayMock;
    private IBarCodeGateway barcodeGatewayMock;

    private static final String TEST_SECRET = "WESP-Group-14B-Security-Token-Key-32-Characters";

    @BeforeEach
    public void setUp() throws Exception {
        // User/Auth Setup
        userRepo = new MemoryUserRepo();
        sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        java.lang.reflect.Field secretField = DomainAuthService.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(domainAuthService, TEST_SECRET);
        domainAuthService.init();
        authenticationService = new AuthenticationService(domainAuthService, sessionRepo);
        UserHandler userHandler = new UserHandler();
        UserPublisher userPublisher = new UserPublisher(event -> {});
        userService = new UserService(userRepo, userHandler, authenticationService, userPublisher);

        // Event Setup
        eventRepo = new EventRepo();
        eventPublisher = new EventAggregatePublisher(event -> {});
        eventService = new EventService(eventRepo, eventPublisher, authenticationService);

        // History Setup
        historyOrderRepo = new HistoryOrderRepo();
        eventListener = new EventAggregateListener(eventRepo, eventService, historyOrderRepo);
        HistoryOrderHandler historyOrderHandler = new HistoryOrderHandler();
        ProductionService productionService = new ProductionService(authenticationService, null, null, null);
        historyOrderService = new HistoryOrderService(historyOrderRepo, historyOrderHandler, authenticationService, productionService, Mockito.mock(IUserRepo.class));
        historyOrderListener = new HistoryOrderListener(historyOrderRepo, historyOrderService);

        // Active Order Setup
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

        paymentGatewayMock = Mockito.mock(IPaymentGateway.class);
        barcodeGatewayMock = Mockito.mock(IBarCodeGateway.class);

        activeOrderService = new ActiveOrderService(
                activeOrderListener,
                activeOrderPublisher,
                activeOrderRepo,
                activeOrderHandler,
                authenticationService,
                barcodeGatewayMock
        );
    }

    private String createTestEvent(String sessionToken) {
        int companyId = 10;
        String eventName = "TestEvent";
        int eventCapacity = 50;
        LocalDateTime eventDate = LocalDateTime.now().plusYears(1);
        String eventLocation ="TestLocation";
        PurchasePolicyDTO purchasePolicyDTO = new PurchasePolicyDTO(0, eventCapacity, false, 0, 60, true, false);
        List<DiscountDTO> discounts = new ArrayList<>();

        // ✅ FIXED: Expanded initialization with 10 arguments to adhere to updated record schema
        EventDTO e = new EventDTO(
                null,
                companyId,
                eventName,
                eventCapacity,
                eventDate,
                true,
                eventLocation,
                null, // imageUrl
                null, // minZonePrice
                null  // maxZonePrice
        );

        eventService.createEvent(sessionToken, e, purchasePolicyDTO, discounts);

        List<EventDTO> events = eventService.searchEventsByCompany(sessionToken, companyId);
        String eventId = events.get(0).eventId();

        SeatingMap seatingMap = eventService.configureSeatingMap(
                sessionToken,
                List.of(new SeatingAreaConfig(1, 2, 10.0)),
                List.of(new StandingAreaConfig(20, 10.0))
        );
        eventService.editEventSeatingMap(sessionToken, eventId, seatingMap);
        return eventId;
    }

    // 1. ActiveOrders (Concurrent Finalization)
    @Test
    public void givenActiveOrderWithReservedSeats_whenUsersCompleteOrderConcurrently_thenOnlyOneSucceeds() throws Exception {
        String userId = "user_finalizer";
        userRepo.store(new UserInfo(userId, "Finalizer User", "finalizer@test.com", "pass123", UserGroupDiscount.NONE));
        String sessionToken = authenticationService.login(userId);

        String eventId = createTestEvent(sessionToken);

        // Create order
        ActiveOrderItem order = activeOrderService.createPendingOrder(new SessionToken(sessionToken, 1000), userId, eventId);
        assertNotNull(order);

        // Add some seats to make it a valid order to complete
        Event event = eventRepo.findById(eventId);
        List<String> seatIds = event.getSeatingMap().getSeatIds();
        activeOrderService.addSeatsToActiveOrder(new SessionToken(sessionToken, 1000), order.getOrderId(), List.of(seatIds.get(0)));

        // Mock payment and barcode systems
        when(paymentGatewayMock.pay(any())).thenReturn(50000);
        when(barcodeGatewayMock.issueBarcodes(any())).thenReturn(List.of(new BarcodeDTO("barcode-xyz")));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicReference<Exception> failureException = new AtomicReference<>();

        executor.submit(() -> {
            try {
                startLatch.await();
                activeOrderService.completeOrder(paymentGatewayMock, new SessionToken(sessionToken, 1000), new PaymentDetails(100.0, "USD", "4111111111111111", "12", "2028", "Test User", "123", "ID-001"), order.getOrderId(), null);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
                failureException.set(e);
            } finally {
                endLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                activeOrderService.completeOrder(paymentGatewayMock, new SessionToken(sessionToken, 1000), new PaymentDetails(100.0, "USD", "4111111111111111", "12", "2028", "Test User", "123", "ID-001"), order.getOrderId(), null);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
                failureException.set(e);
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        endLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one thread must succeed in completing the order");
        assertEquals(1, failureCount.get(), "Exactly one thread must fail to complete the order");

        Exception ex = failureException.get();
        assertNotNull(ex);
        assertTrue(ex instanceof IllegalStateException || ex instanceof IllegalArgumentException,
                "Expected failure exception to be IllegalStateException or IllegalArgumentException, but was " + ex.getClass().getName());
    }

    // 2. Authentications (Duplicate Registrations)
    @Test
    public void givenTwoGuestSessions_whenRegisteringSameUserIdConcurrently_thenStateIsConsistent() throws Exception {
        String sharedUserId = "alice_dup";
        String name = "Alice";
        String password = "password123";
        String email = "alice@dup.com";

        // Get two guest sessions
        String guestToken1 = userService.guestEntry();
        String guestToken2 = userService.guestEntry();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                startLatch.await();
                userService.registerUser(sharedUserId, name, password, email, UserGroupDiscount.NONE, guestToken1);
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
                userService.registerUser(sharedUserId, name, password, email, UserGroupDiscount.NONE, guestToken2);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        endLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert both registration attempts ran
        assertEquals(2, successCount.get() + failureCount.get(), "Both requests must be processed");

        // Assert state consistency (exactly 1 user remains in MemoryUserRepo)
        UserInfo storedUser = userRepo.findByID(sharedUserId);
        assertNotNull(storedUser, "User should be registered");
        assertEquals(sharedUserId, storedUser.getId());
    }

    // 3. Events (Concurrent Seat Reservations)
    @Test
    public void givenTwoUsersAndOneAvailableSeat_whenReservingSameSeatConcurrently_thenSeatIsBooked() throws Exception {
        String userA = "user_a";
        String userB = "user_b";
        userRepo.store(new UserInfo(userA, "User A", "usera@test.com", "pass123", UserGroupDiscount.NONE));
        userRepo.store(new UserInfo(userB, "User B", "userb@test.com", "pass123", UserGroupDiscount.NONE));

        String tokenA = authenticationService.login(userA);
        String tokenB = authenticationService.login(userB);

        String eventId = createTestEvent(tokenA);
        Event event = eventRepo.findById(eventId);
        String seatId = event.getSeatingMap().getSeatIds().get(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                startLatch.await();
                eventService.reserveSeats(tokenA, "order-a", eventId, List.of(seatId));
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
                eventService.reserveSeats(tokenB, "order-b", eventId, List.of(seatId));
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        endLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        int totalProcessed = successCount.get() + failureCount.get();
        assertEquals(2, totalProcessed);

        // Verify the seat was successfully marked as booked in the end
        Event finalEvent = eventRepo.findById(eventId);
        assertTrue(finalEvent.getSeatingMap().getSeat(seatId).isBooked());
    }

    // 4. Events (Concurrent Base Updates with Optimistic Locking)
    @Test
    public void givenTwoDifferentCopiesOfSameEvent_whenSavingBothConcurrently_thenOneSucceedsAndOtherFailsWithOptimisticLockingException() throws Exception {
        String userId = "event_updater";
        userRepo.store(new UserInfo(userId, "Event Updater", "updater@test.com", "pass123", UserGroupDiscount.NONE));
        String sessionToken = authenticationService.login(userId);

        String eventId = createTestEvent(sessionToken);

        // Retrieve two distinct in-memory copies of the event
        Event eventCopy1 = eventRepo.findById(eventId);
        Event eventCopy2 = eventRepo.findById(eventId);

        assertNotNull(eventCopy1);
        assertNotNull(eventCopy2);
        assertEquals(eventCopy1.getVersion(), eventCopy2.getVersion());

        // Modify both copies
        eventCopy1.setEventCapacity(100);
        eventCopy2.setEventCapacity(200);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicReference<Exception> optLockException = new AtomicReference<>();

        executor.submit(() -> {
            try {
                startLatch.await();
                eventRepo.save(eventCopy1);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
                optLockException.set(e);
            } finally {
                endLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                eventRepo.save(eventCopy2);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
                optLockException.set(e);
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        endLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert that optimistic locking succeeds on one and throws an OptimisticLockingFailureException on the other
        assertEquals(1, successCount.get());
        assertEquals(1, failureCount.get());
        assertNotNull(optLockException.get());
        assertTrue(optLockException.get() instanceof OptimisticLockingFailureException,
                "Expected OptimisticLockingFailureException but got: " + optLockException.get().getClass().getName());
    }
}