package com.ticketpurchasingsystem.project.Controllers;

import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/history-order")
@CrossOrigin(origins = "*")
public class HistoryOrderController {

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserOrders(
            @PathVariable String userId,
            @RequestHeader(value = "Authorization", required = false) String token) throws InterruptedException {

        // 1. מצב טעינה (Loading) - הוספת דיליי מלאכותי להמחשה
        if ("loading".equals(userId)) {
            Thread.sleep(3000); // 3 שניות של חשיבה
        } else {
            Thread.sleep(600); // דיליי טבעי קצר
        }

        // 2. מצב שגיאה (Error 403 Forbidden)
        if ("error".equals(userId)) {
            return ResponseEntity.status(403).body(new ErrorResponse("Access Forbidden"));
        }

        // 3. מצב ריק (Empty State)
        if ("empty".equals(userId)) {
            return ResponseEntity.ok(Arrays.asList());
        }

        // 4. מצב הצלחה (Success State) - נתוני דמה
        HashMap<String, Integer> standing1 = new HashMap<>();
        standing1.put("Standing", 1);
        HistoryOrderDTO order1 = new HistoryOrderDTO(
                "ORD-98231", userId, "EVT-ZENITH", 1,
                new Timestamp(System.currentTimeMillis() - (86400000L * 5)), 150.00,
                Arrays.asList("A1", "A2"), standing1
        );

        HashMap<String, Integer> standing2 = new HashMap<>();
        HistoryOrderDTO order2 = new HistoryOrderDTO(
                "ORD-77622", userId, "EVT-TECH", 2,
                new Timestamp(System.currentTimeMillis() - (86400000L * 30)), 499.00,
                Arrays.asList("V1"), standing2
        );

        HashMap<String, Integer> standing3 = new HashMap<>();
        standing3.put("General", 3);
        HistoryOrderDTO order3 = new HistoryOrderDTO(
                "ORD-55410", userId, "EVT-ART", 1,
                new Timestamp(System.currentTimeMillis() - (86400000L * 120)), 45.00,
                Arrays.asList(), standing3
        );

        return ResponseEntity.ok(Arrays.asList(order1, order2, order3));
    }

    // מחלקת עזר פנימית לשליחת שגיאות בפורמט JSON נקי
    static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) { this.error = error; }
    }
}