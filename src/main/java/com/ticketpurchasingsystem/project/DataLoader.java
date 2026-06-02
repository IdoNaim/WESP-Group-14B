package com.ticketpurchasingsystem.project;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.Controllers.HistoryOrderController;
import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.application.HistoryOrderService;
import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;

import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.application.ActiveOrderService;
import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.application.HistoryOrderService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.application.UserService.UserService;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingMap;

@Component
public class DataLoader implements ApplicationRunner {

    private final UserService userService;
    private final ProductionService productionService;
    private final EventService eventService;
    private final ActiveOrderService activeOrderService;
        private final HistoryOrderService historyOrderService;

    public DataLoader(UserService userService, ProductionService productionService, EventService eventService, ActiveOrderService activeOrderService, HistoryOrderService historyOrderService) {
        this.userService = userService;
        this.productionService = productionService;
        this.eventService = eventService;
        this.activeOrderService = activeOrderService;
        this.historyOrderService = historyOrderService;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Step 1: enter as guest, register + login user1
        String guestToken1 = userService.guestEntry();
        userService.registerUser("idonaim56@gmail.com", "ido", "pass123", "idonaim56@gmail.com",
                UserGroupDiscount.NONE, guestToken1);
        String idoToken = userService.loginUser("idonaim56@gmail.com", "pass123", guestToken1);



        // Step 3: alice creates a production company
        ProductionCompanyDTO companyDTO = new ProductionCompanyDTO(
                "Live Events Co.",
                "Premier live event organizer",
                "contact@liveevents.com"
        );
        Integer companyId = productionService.createProductionCompany(idoToken, companyDTO);


        ProductionCompanyDTO companyDTO2 = new ProductionCompanyDTO(
                "Comedy Central",
                "Leading comedy event organizer",
                "contact@comedycentral.com"
        );
        Integer companyId2 = productionService.createProductionCompany(aliceToken, companyDTO2);


        //appoint admin as manager to the company for testing purposes
        Set<ManagerPermission> permissions = new HashSet<>();
        permissions.add(ManagerPermission.PURCHASE_AND_ORDER_HISTORY_ACCESS);
        productionService.appointManager(adminGuestToken, companyId, "admin@gmail.com", permissions);
        
        if (companyId == null) {
            return;
        }

        // Step 4: create some events under that company
        PurchasePolicyDTO noRestrictions = new PurchasePolicyDTO(
                null, null, false,
                null, null, false,
                false
        );

        PurchasePolicyDTO adultOnly = new PurchasePolicyDTO(
                null, null, false,
                18, null, false,
                false
        );

        List<DiscountDTO> noDiscounts = List.of();

        eventService.createEvent(idoToken,
                new EventDTO(null, companyId, "Rock Night", 500,
                        LocalDateTime.now().plusDays(30), true, "Tel Aviv Arena", 120.0),
                adultOnly, noDiscounts);
        SeatingMap seatingMap = new SeatingMap();
        seatingMap.addSeatingArea(10, 10, 120.0);
        seatingMap.addStandingArea(100, 80.0);
        eventService.editEventSeatingMap(idoToken, "1", seatingMap);
        List<EventDTO> events = eventService.searchEventsByCompany(idoToken, companyId);
        String eventId = events.get(0).eventId();
        System.out.println("Created event with ID: " + eventId);
        SessionToken sessionToken = new SessionToken(idoToken, 1000);
        String idoOrderId = activeOrderService.createPendingOrder(sessionToken, "idonaim56@gmail.com", "1").getOrderId();
        activeOrderService.addSeatsToActiveOrder(sessionToken, idoOrderId, List.of("0_1_1"));
        activeOrderService.addStandingAreaToActiveOrder(sessionToken, idoOrderId, "1", 2);
        
        historyOrderService.createHistoryOrder("order1", "idonaim56@gmail.com", "1", companyId, new Timestamp(System.currentTimeMillis()), 100.0, List.of("A1", "A2"), new HashMap<>());

        historyOrderService.createHistoryOrder("order2", "idonaim56@gmail.com", "3", companyId2, new Timestamp(System.currentTimeMillis()), 100.0, List.of("A1", "A2"), new HashMap<>());

        // Leave users logged out so they can login normally from the frontend
        userService.logoutUser("idonaim56@gmail.com", idoToken);
//        String guestToken1 = userService.guestEntry();
//        userService.registerUser("alice", "Alice Smith", "pass123", "alice@example.com",
//                UserGroupDiscount.NONE, guestToken1);
//        String aliceToken = userService.loginUser("alice", "pass123", guestToken1);
//
//        // Step 2: enter as guest, register + login user2
//        String guestToken2 = userService.guestEntry();
//        userService.registerUser("bob", "Bob Jones", "pass456", "bob@example.com",
//                UserGroupDiscount.STUDENT, guestToken2);
//        String bobToken = userService.loginUser("bob", "pass456", guestToken2);
//
//        // Step 3: alice creates a production company
//        ProductionCompanyDTO companyDTO = new ProductionCompanyDTO(
//                "Live Events Co.",
//                "Premier live event organizer",
//                "contact@liveevents.com"
//        );
//        Integer companyId = productionService.createProductionCompany(aliceToken, companyDTO);
//
//        if (companyId == null) {
//            return;
//        }
//
//        // Step 4: create some events under that company
//        PurchasePolicyDTO noRestrictions = new PurchasePolicyDTO(
//                null, null, false,
//                null, null, false,
//                false
//        );
//
//        PurchasePolicyDTO adultOnly = new PurchasePolicyDTO(
//                null, null, false,
//                18, null, false,
//                false
//        );
//
//        List<DiscountDTO> noDiscounts = List.of();
//
//        eventService.createEvent(aliceToken,
//                new EventDTO(null, companyId, "Rock Night", 500,
//                        LocalDateTime.now().plusDays(30), true, "Tel Aviv Arena", 120.0),
//                noRestrictions, noDiscounts);
//
//        eventService.createEvent(aliceToken,
//                new EventDTO(null, companyId, "Jazz Evening", 200,
//                        LocalDateTime.now().plusDays(14), true, "Haifa Jazz Club", 80.0),
//                noRestrictions, noDiscounts);
//
//        eventService.createEvent(aliceToken,
//                new EventDTO(null, companyId, "Comedy Night 18+", 300,
//                        LocalDateTime.now().plusDays(7), true, "Jerusalem Theater", 60.0),
//                adultOnly, noDiscounts);
//
//        // Leave users logged out so they can login normally from the frontend
//        userService.logoutUser("alice", aliceToken);
//        userService.logoutUser("bob", bobToken);
    }
}
