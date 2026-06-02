package com.ticketpurchasingsystem.project;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.application.NotificationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.application.UserService.UserService;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingAreaConfig;
import com.ticketpurchasingsystem.project.domain.event.Maps.StandingAreaConfig;

@Component
public class DataLoader implements ApplicationRunner {

    private final UserService userService;
    private final ProductionService productionService;
    private final EventService eventService;
    private final NotificationService notificationService;

    public DataLoader(UserService userService, ProductionService productionService, EventService eventService, NotificationService notificationService) {
        this.userService = userService;
        this.productionService = productionService;
        this.eventService = eventService;
        this.notificationService = notificationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Step 1: enter as guest, register + login user1
        String guestToken1 = userService.guestEntry();
        userService.registerUser("alice", "Alice Smith", "pass123", "alice@example.com",
                UserGroupDiscount.NONE, guestToken1);
        String aliceToken = userService.loginUser("alice", "pass123", guestToken1);

        // Step 2: enter as guest, register + login user2
        String guestToken2 = userService.guestEntry();
        userService.registerUser("bob", "Bob Jones", "pass456", "bob@example.com",
                UserGroupDiscount.STUDENT, guestToken2);
        String bobToken = userService.loginUser("bob", "pass456", guestToken2);

        // Step 3: alice creates a production company
        ProductionCompanyDTO companyDTO = new ProductionCompanyDTO(
                "Live Events Co.",
                "Premier live event organizer",
                "contact@liveevents.com"
        );
        Integer companyId = productionService.createProductionCompany(aliceToken, companyDTO);

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

        eventService.createEvent(aliceToken,
                new EventDTO(null, companyId, "Rock Night", 500,
                        LocalDateTime.now().plusDays(30), true, "Tel Aviv Arena", 120.0),
                noRestrictions, noDiscounts);

        eventService.createEvent(aliceToken,
                new EventDTO(null, companyId, "Jazz Evening", 200,
                        LocalDateTime.now().plusDays(14), true, "Haifa Jazz Club", 80.0),
                noRestrictions, noDiscounts);

        eventService.createEvent(aliceToken,
                new EventDTO("1", companyId, "Comedy Night 18+", 300,
                        LocalDateTime.now().plusDays(7), true, "Jerusalem Theater", 60.0),
                adultOnly, noDiscounts);
        
        

        addSeatingMapToEvent(aliceToken, "1",
                List.of(new SeatingAreaConfig(10, 20, 120.0)),
                List.of(new StandingAreaConfig(100, 60.0)));

        // Leave users logged out so they can login normally from the frontend
        userService.logoutUser("alice", aliceToken);
        userService.logoutUser("bob", bobToken);

        //notification test
        notificationService.createSystemNotification("alice", "Welcome to the ticket purchasing system, Alice!");
        notificationService.createSystemNotification("bob", "Welcome to the ticket purchasing system, Bob!");

    }

    private void addSeatingMapToEvent(String token, String eventId,
                                      List<SeatingAreaConfig> seatingAreas,
                                      List<StandingAreaConfig> standingAreas) {
        var map = eventService.configureSeatingMap(token, seatingAreas, standingAreas);
        eventService.editEventSeatingMap(token, eventId, map);
    }
}
