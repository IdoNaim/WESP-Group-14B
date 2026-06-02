package com.ticketpurchasingsystem.project;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.Controllers.HistoryOrderController;
import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.application.HistoryOrderService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.application.UserService.UserService;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

@Component
public class DataLoader implements ApplicationRunner {

    private final UserService userService;
    private final ProductionService productionService;
    private final EventService eventService;
    private final HistoryOrderService historyOrderService;
    
    public DataLoader(UserService userService, ProductionService productionService, EventService eventService, HistoryOrderService historyOrderService) {
        this.userService = userService;
        this.productionService = productionService;
        this.eventService = eventService;
        this.historyOrderService = historyOrderService;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Step 1: enter as guest, register + login user1 (Alice)
        String guestToken1 = userService.guestEntry();
        userService.registerUser("alice", "Alice Smith", "pass123", "alice@example.com",
                UserGroupDiscount.NONE, guestToken1);
        String aliceToken = userService.loginUser("alice", "pass123", guestToken1);

        // Step 2: enter as guest, register + login user2 (Bob)
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
                null, null, false, null, null, false, false
        );

        PurchasePolicyDTO adultOnly = new PurchasePolicyDTO(
                null, null, false, 18, null, false, false
        );

        List<DiscountDTO> noDiscounts = List.of();

        eventService.createEvent(aliceToken,
                new EventDTO("1", companyId, "Rock Night", 500,
                        LocalDateTime.now().plusDays(30), true, "Tel Aviv Arena", 120.0),
                noRestrictions, noDiscounts);

        eventService.createEvent(aliceToken,
                new EventDTO("2", companyId, "Jazz Evening", 200,
                        LocalDateTime.now().plusDays(14), true, "Haifa Jazz Club", 80.0),
                noRestrictions, noDiscounts);

        eventService.createEvent(aliceToken,
                new EventDTO("3", companyId, "Comedy Night 18+", 300,
                        LocalDateTime.now().plusDays(7), true, "Jerusalem Theater", 60.0),
                adultOnly, noDiscounts);

        // ====================================================================================
        // === התיקון: יוצרים את ההזמנה לפני שמנסים לשלוף אותה ===============================
        // ====================================================================================
        
        // 1. קודם מוסיפים את ההזמנה להיסטוריה של Bob
        historyOrderService.createHistoryOrder("order1", "bob", "1", companyId, new Timestamp(System.currentTimeMillis()), 100.0, List.of("A1", "A2"), new HashMap<>());
        loggerDef.getInstance().info("Successfully created history order 'order1' for bob.");

        // 2. עכשיו בודקים את ה-Controller (שולפים את ההזמנה שכבר קיימת)
        HistoryOrderController historyOrderController = new HistoryOrderController(historyOrderService);
        //get all by company
        ResponseEntity<?> response = historyOrderController.getOrdersByCompany("Bearer " + aliceToken, companyId);
        loggerDef.getInstance().info("History Order Response: " + response.getBody());

        // 3. מתנתקים (כדי שיוכלו להתחבר מה-React)
        userService.logoutUser("alice", aliceToken);
        userService.logoutUser("bob", bobToken);

        loggerDef.getInstance().info("Data loading completed.=================================================================");
    }
}