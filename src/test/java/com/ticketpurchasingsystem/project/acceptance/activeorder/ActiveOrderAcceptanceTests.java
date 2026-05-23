package com.ticketpurchasingsystem.project.acceptance.activeorder;

import com.ticketpurchasingsystem.project.application.*;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderHandler;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderListener;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderPublisher;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.IActiveOrderRepo;
import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import com.ticketpurchasingsystem.project.domain.event.EventAggregateListener;
import com.ticketpurchasingsystem.project.domain.event.EventAggregatePublisher;
import com.ticketpurchasingsystem.project.infrastructure.ActiveOrderMemRepo;
import com.ticketpurchasingsystem.project.infrastructure.BarCodeGateway;
import com.ticketpurchasingsystem.project.infrastructure.EventRepo;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
@SpringBootTest(classes = {
        ActiveOrderService.class,
        ActiveOrderPublisher.class,
        ActiveOrderListener.class,
        ActiveOrderMemRepo.class,
        InMemorySessionRepo.class,
        AuthenticationService.class,
        DomainAuthService.class,
        ActiveOrderHandler.class,
        EventService.class,
        EventRepo.class,
        EventAggregatePublisher.class,
        EventAggregateListener.class
})
public class ActiveOrderAcceptanceTests {
    @Autowired
    private IActiveOrderService activeOrderService;
    @Autowired
    private IActiveOrderRepo activeOrderRepo;
    @Autowired
    private ISessionRepo sessionRepo;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private IEventService eventService;
    //external systems
    @MockBean
    private IPaymentGateway paymentGatewayMock;
    @MockBean
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

        // Cast or use the explicit bean to wipe out state cleanly
        ((ActiveOrderMemRepo) activeOrderRepo).clear();
    }
    @Test
    public void checkSetup(){
        assertTrue(true);
        System.out.println("setup complete!");
    }
    @Test
    public void GivenValidEventId_WhenCreateOrder_ThenCreateActiveOrderSuccessfully(){
        registeredUsers.add(USER1_ID);
        String sessionToken = authenticationService.login(USER1_ID);
        EventDTO e = new EventDTO(companyId,
                eventName,
                eventCapacity,
                eventDate,
        true);
        eventService.createEvent(e, purchasePolicyDTO, discounts);
//        activeOrderService.createPendingOrder(new SessionToken(sessionToken, 1000),USER1_ID ,eventId);
    }

}
