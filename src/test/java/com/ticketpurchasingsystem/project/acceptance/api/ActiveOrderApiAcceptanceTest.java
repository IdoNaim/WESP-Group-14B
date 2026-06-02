package com.ticketpurchasingsystem.project.acceptance.api;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketpurchasingsystem.project.Controllers.ActiveOrderController;
import com.ticketpurchasingsystem.project.Controllers.apidto.AddSeatsRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.AddStandingAreaRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.CheckoutRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.CreateOrderRequestDTO;
import com.ticketpurchasingsystem.project.application.ActiveOrderService;
import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.application.HistoryOrderService;
import com.ticketpurchasingsystem.project.application.IActiveOrderService;
import com.ticketpurchasingsystem.project.application.IBarCodeGateway;
import com.ticketpurchasingsystem.project.application.IEventService;
import com.ticketpurchasingsystem.project.application.IPaymentGateway;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.CompletedOrderEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.GetCompanyIdEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.IsUpToPolicyEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.IsValidEventIDEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.SeatReleaseEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.SeatReservationEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.StandingAreaReleaseEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.StandingAreaReservationEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderHandler;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderListener;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderPublisher;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.BarcodeDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.IActiveOrderRepo;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderHandler;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderListener;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;
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

class ActiveOrderApiAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";
    private static final String USER_ID = "api-test-user-123";

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private IActiveOrderService activeOrderService;
    private IEventService eventService;
    private IActiveOrderRepo activeOrderRepo;

    private IPaymentGateway paymentGatewayMock;
    private IBarCodeGateway barCodeGatewayMock;

    private String validAuthHeader;
    private String userToken;
    private String eventId;
    private List<String> seatIds;
    private List<String> areaIds;

    @BeforeEach
    void setUp() {
        paymentGatewayMock = mock(IPaymentGateway.class);
        barCodeGatewayMock = mock(IBarCodeGateway.class);

        ISessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        AuthenticationService authService = new AuthenticationService(domainAuthService, sessionRepo);
        ReflectionTestUtils.setField(domainAuthService, "secret", TEST_SECRET);
        domainAuthService.init();
        userToken = authService.login(USER_ID);
        validAuthHeader = "Bearer " + userToken;

        IEventRepo eventRepo = new EventRepo();
        EventAggregatePublisher eventPublisher = new EventAggregatePublisher(e -> {});
        eventService = new EventService(eventRepo, eventPublisher, authService);
        EventAggregateListener eventListener = new EventAggregateListener(eventRepo, eventService);

        IHistoryOrderRepo historyRepo = new HistoryOrderRepo();
        HistoryOrderHandler historyOrderHandler = new HistoryOrderHandler();
        ProductionService productionServiceStub = mock(ProductionService.class);
        HistoryOrderService historyOrderService = new HistoryOrderService(
                historyRepo, historyOrderHandler, authService, productionServiceStub);
        HistoryOrderListener historyOrderListener = new HistoryOrderListener(historyRepo, historyOrderService);

        activeOrderRepo = new ActiveOrderMemRepo();
        ActiveOrderHandler activeOrderHandler = new ActiveOrderHandler();
        ActiveOrderListener activeOrderListener = new ActiveOrderListener(activeOrderRepo);
        ActiveOrderPublisher activeOrderPublisher = new ActiveOrderPublisher(event -> {
            if (event instanceof IsValidEventIDEvent e)      eventListener.onIsValidEventIDEvent(e);
            else if (event instanceof GetCompanyIdEvent e)   eventListener.onGetCompanyIdEvent(e);
            else if (event instanceof IsUpToPolicyEvent e)   eventListener.onIsUpToPolicyEvent(e);
            else if (event instanceof CompletedOrderEvent e) historyOrderListener.onCompletedOrder(e);
            else if (event instanceof SeatReleaseEvent e)    eventListener.onSeatReleaseEvent(e);
            else if (event instanceof SeatReservationEvent e) eventListener.onSeatReservationEvent(e);
            else if (event instanceof StandingAreaReleaseEvent e)    eventListener.onStandingAreaReleaseEvent(e);
            else if (event instanceof StandingAreaReservationEvent e) eventListener.onStandingAreaReservationEvent(e);
        });
        activeOrderService = new ActiveOrderService(
                activeOrderListener, activeOrderPublisher, activeOrderRepo,
                activeOrderHandler, authService, barCodeGatewayMock);

        mockMvc = MockMvcBuilders.standaloneSetup(
                new ActiveOrderController(activeOrderService, paymentGatewayMock)).build();
        objectMapper = new ObjectMapper();

        PurchasePolicyDTO policy = new PurchasePolicyDTO(0, 10, false, 0, 100, false, false);
        List<DiscountDTO> discounts = Collections.emptyList();
        EventDTO eventDTO = new EventDTO(null, 1, "Test Event", 100,
                LocalDateTime.now().plusDays(30), "test location", true);
        eventService.createEvent(userToken, eventDTO, policy, discounts);
        List<EventDTO> events = eventService.searchEventsByCompany(userToken, 1);
        eventId = events.get(0).eventId();

        SeatingMap seatingMap = eventService.configureSeatingMap(userToken,
                List.of(new SeatingAreaConfig(1, 2, 10.0)),
                List.of(new StandingAreaConfig(20, 5.0)));
        seatIds = new ArrayList<>(seatingMap.getSeatIds());
        areaIds = new ArrayList<>(seatingMap.getAreaIds());
        eventService.editEventSeatingMap(userToken, eventId, seatingMap);
    }

    // POST /api/orders

    @Test
    void GivenValidEventId_WhenCreateOrder_ThenReturn201WithOrderDetails() throws Exception {
        CreateOrderRequestDTO body = new CreateOrderRequestDTO();
        body.setUserId(USER_ID);
        body.setEventId(eventId);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", validAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.eventId").value(eventId));
    }

    @Test
    void GivenNonExistentEventId_WhenCreateOrder_ThenReturn401() throws Exception {
        CreateOrderRequestDTO body = new CreateOrderRequestDTO();
        body.setUserId(USER_ID);
        body.setEventId("non-existent-event-id");

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", validAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GivenUserAlreadyHasOrder_WhenCreateOrder_ThenReturn400() throws Exception {
        createOrder();

        CreateOrderRequestDTO body = new CreateOrderRequestDTO();
        body.setUserId(USER_ID);
        body.setEventId(eventId);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", validAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // GET /api/orders/{orderId}

    @Test
    void GivenCreatedOrder_WhenGetActiveOrder_ThenReturn200WithData() throws Exception {
        String orderId = createOrder();

        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    @Test
    void GivenNonExistentOrderId_WhenGetActiveOrder_ThenReturn404() throws Exception {
        mockMvc.perform(get("/api/orders/non-existent-order")
                        .header("Authorization", validAuthHeader))
                .andExpect(status().isNotFound());
    }

    // POST /api/orders/{orderId}/seats

    @Test
    void GivenValidSeatIds_WhenAddSeats_ThenReturn200() throws Exception {
        String orderId = createOrder();
        AddSeatsRequestDTO body = new AddSeatsRequestDTO();
        body.setSeatIds(seatIds);

        mockMvc.perform(post("/api/orders/" + orderId + "/seats")
                        .header("Authorization", validAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Seats added successfully."));
    }

    @Test
    void GivenNonExistentSeatIds_WhenAddSeats_ThenReturn409() throws Exception {
        String orderId = createOrder();
        AddSeatsRequestDTO body = new AddSeatsRequestDTO();
        body.setSeatIds(List.of("non-existent-seat-id"));

        mockMvc.perform(post("/api/orders/" + orderId + "/seats")
                        .header("Authorization", validAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    @Test
    void GivenAlreadyBookedSeats_WhenAddSeats_ThenReturn409() throws Exception {
        String orderId = createOrder();
        // Book the seats first via the event service directly
        eventService.reserveSeats(userToken, "other-order", eventId, seatIds);

        AddSeatsRequestDTO body = new AddSeatsRequestDTO();
        body.setSeatIds(seatIds);

        mockMvc.perform(post("/api/orders/" + orderId + "/seats")
                        .header("Authorization", validAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    // POST /api/orders/{orderId}/standing

    @Test
    void GivenValidAreaId_WhenAddStandingArea_ThenReturn200() throws Exception {
        String orderId = createOrder();
        AddStandingAreaRequestDTO body = new AddStandingAreaRequestDTO();
        body.setAreaId(areaIds.get(0));
        body.setQuantity(2);

        mockMvc.perform(post("/api/orders/" + orderId + "/standing")
                        .header("Authorization", validAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Standing area tickets added successfully."));
    }

    @Test
    void GivenNonExistentAreaId_WhenAddStandingArea_ThenReturn409() throws Exception {
        String orderId = createOrder();
        AddStandingAreaRequestDTO body = new AddStandingAreaRequestDTO();
        body.setAreaId("non-existent-area");
        body.setQuantity(2);

        mockMvc.perform(post("/api/orders/" + orderId + "/standing")
                        .header("Authorization", validAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    @Test
    void GivenInsufficientCapacity_WhenAddStandingArea_ThenReturn409() throws Exception {
        String orderId = createOrder();
        AddStandingAreaRequestDTO body = new AddStandingAreaRequestDTO();
        body.setAreaId(areaIds.get(0));
        body.setQuantity(99999);

        mockMvc.perform(post("/api/orders/" + orderId + "/standing")
                        .header("Authorization", validAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    // DELETE /api/orders/{orderId}

    @Test
    void GivenExistingOrder_WhenCancelOrder_ThenReturn200() throws Exception {
        String orderId = createOrder();

        mockMvc.perform(delete("/api/orders/" + orderId)
                        .header("Authorization", validAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order cancelled successfully."));
    }

    @Test
    void GivenNonExistentOrder_WhenCancelOrder_ThenReturn400() throws Exception {
        mockMvc.perform(delete("/api/orders/fake-order-id")
                        .header("Authorization", validAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", USER_ID))))
                .andExpect(status().isBadRequest());
    }

    // POST /api/orders/{orderId}/checkout

    @Test
    void GivenValidOrderWithSeats_WhenCheckout_ThenReturn200WithBarcodes() throws Exception {
        when(paymentGatewayMock.pay(any())).thenReturn(50000);
        when(barCodeGatewayMock.issueBarcodes(any())).thenReturn(List.of(new BarcodeDTO("barcode-1")));

        String orderId = createOrder();
        AddSeatsRequestDTO seats = new AddSeatsRequestDTO();
        seats.setSeatIds(seatIds);
        mockMvc.perform(post("/api/orders/" + orderId + "/seats")
                .header("Authorization", validAuthHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(seats)));

        CheckoutRequestDTO checkout = new CheckoutRequestDTO();
        checkout.setAmount(100.0);

        mockMvc.perform(post("/api/orders/" + orderId + "/checkout")
                        .header("Authorization", validAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkout)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barcodes").isArray())
                .andExpect(jsonPath("$.barcodes[0]").value("barcode-1"));
    }

    @Test
    void GivenPaymentFails_WhenCheckout_ThenReturn409() throws Exception {
        when(paymentGatewayMock.pay(any())).thenReturn(-1);

        String orderId = createOrder();
        CheckoutRequestDTO checkout = new CheckoutRequestDTO();
        checkout.setAmount(100.0);

        mockMvc.perform(post("/api/orders/" + orderId + "/checkout")
                        .header("Authorization", validAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkout)))
                .andExpect(status().isConflict());
    }

    @Test
    void GivenOrderViolatesPurchasePolicy_WhenCheckout_ThenReturn409() throws Exception {
        // Change policy to require min 10 tickets, but order has 2 seats
        PurchasePolicyDTO strictPolicy = new PurchasePolicyDTO(10, 50, false, 0, 100, false, false);
        eventService.editEventPurchasePolicy(userToken, eventId, strictPolicy);

        String orderId = createOrder();
        AddSeatsRequestDTO seats = new AddSeatsRequestDTO();
        seats.setSeatIds(seatIds); // only 2 seats
        mockMvc.perform(post("/api/orders/" + orderId + "/seats")
                .header("Authorization", validAuthHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(seats)));

        CheckoutRequestDTO checkout = new CheckoutRequestDTO();
        checkout.setAmount(20.0);

        mockMvc.perform(post("/api/orders/" + orderId + "/checkout")
                        .header("Authorization", validAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkout)))
                .andExpect(status().isConflict());
    }

    private String createOrder() throws Exception {
        CreateOrderRequestDTO body = new CreateOrderRequestDTO();
        body.setUserId(USER_ID);
        body.setEventId(eventId);

        String response = mockMvc.perform(post("/api/orders")
                        .header("Authorization", validAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("orderId").asText();
    }
}
