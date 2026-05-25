package com.ticketpurchasingsystem.project.Controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ticketpurchasingsystem.project.application.IHistoryOrderService;
import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

@RestController
@RequestMapping("/api/history")
public class HistoryOrderController {

    private final IHistoryOrderService historyOrderService;

    public HistoryOrderController(IHistoryOrderService historyOrderService) {
        this.historyOrderService = historyOrderService;
    }

    // GET /api/history/{orderId}
    // Returns a single completed order by its ID.
    // Accessible by the order's owner or an admin.
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getHistoryOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String orderId) {

        SessionToken sessionToken = toSessionToken(authHeader);
        HistoryOrderDTO order = historyOrderService.getHistoryOrder(sessionToken, orderId);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Order not found or access denied."));
        }
        return ResponseEntity.ok(order);
    }

    // GET /api/history?userId={userId}
    // Returns all completed orders for a specific user.
    // Accessible by the user themselves or an admin.
    @GetMapping(params = "userId")
    public ResponseEntity<List<HistoryOrderDTO>> getOrdersByUser(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String userId) {

        SessionToken sessionToken = toSessionToken(authHeader);
        List<HistoryOrderDTO> orders = historyOrderService.getAllHistoryOrdersByUser(sessionToken, userId);
        return ResponseEntity.ok(orders);
    }

    // GET /api/history?companyId={companyId}
    // Returns all completed orders for a specific production company.
    // Accessible by company owners/founders.
    @GetMapping(params = "companyId")
    public ResponseEntity<List<HistoryOrderDTO>> getOrdersByCompany(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam int companyId) {

        SessionToken sessionToken = toSessionToken(authHeader);
        List<HistoryOrderDTO> orders = historyOrderService.getAllHistoryOrdersByCompany(sessionToken, companyId);
        return ResponseEntity.ok(orders);
    }

    // GET /api/history
    // Returns all completed orders in the system. Admin only.
    @GetMapping
    public ResponseEntity<List<HistoryOrderDTO>> getAllOrders(
            @RequestHeader("Authorization") String authHeader) {

        SessionToken sessionToken = toSessionToken(authHeader);
        List<HistoryOrderDTO> orders = historyOrderService.getAllHistoryOrders(sessionToken);
        return ResponseEntity.ok(orders);
    }

    private SessionToken toSessionToken(String authHeader) {
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : authHeader;
        return new SessionToken(token, Long.MAX_VALUE);
    }
}
