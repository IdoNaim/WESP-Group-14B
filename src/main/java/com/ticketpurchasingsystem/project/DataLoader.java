package com.ticketpurchasingsystem.project;

import java.time.LocalDateTime;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingMap;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.application.ActiveOrderService;
import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.application.UserService.UserService;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;

@Component
public class DataLoader implements ApplicationRunner {

    private final UserService userService;
    private final ProductionService productionService;
    private final EventService eventService;
    private final ActiveOrderService activeOrderService;

    public DataLoader(UserService userService, ProductionService productionService, EventService eventService, ActiveOrderService activeOrderService) {
        this.userService = userService;
        this.productionService = productionService;
        this.eventService = eventService;
        this.activeOrderService = activeOrderService;
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
