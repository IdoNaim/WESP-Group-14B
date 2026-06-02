package com.ticketpurchasingsystem.project.Controllers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticketpurchasingsystem.project.Controllers.apidto.AddSeatsRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.AddStandingAreaRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.CheckoutRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.CreateOrderRequestDTO;
import com.ticketpurchasingsystem.project.application.IActiveOrderService;
import com.ticketpurchasingsystem.project.application.IPaymentGateway;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.BarcodeDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PaymentDetailsDTO;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

@RestController
@RequestMapping("/api/orders")
public class ActiveOrderController {

    private final IActiveOrderService activeOrderService;
    private final IPaymentGateway paymentGateway;

    public ActiveOrderController(IActiveOrderService activeOrderService, IPaymentGateway paymentGateway) {
        this.activeOrderService = activeOrderService;
        this.paymentGateway = paymentGateway;
    }

    // POST /api/orders
    // Creates a new pending order for the authenticated user.
    // Body: { "userId": "...", "eventId": "..." }
    // Returns 201: { "orderId": "...", "userId": "...", "eventId": "..." }
    @PostMapping
    public ResponseEntity<Map<String, String>> createOrder(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateOrderRequestDTO body) {

        SessionToken sessionToken = toSessionToken(authHeader);
        try {
            ActiveOrderItem order = activeOrderService.createPendingOrder(
                    sessionToken, body.getUserId(), body.getEventId());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "orderId", order.getOrderId(),
                    "userId", order.getUserId(),
                    "eventId", order.getEventId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/orders/{orderId}
    // Returns the active order details for the authenticated user.
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getActiveOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String orderId) {

        SessionToken sessionToken = toSessionToken(authHeader);
        try {
            ActiveOrderDTO order = activeOrderService.getActiveOrderInfo(sessionToken, orderId);
            return ResponseEntity.ok(order);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/orders/{orderId}
    // Cancels the active order and releases all reserved tickets.
    // Body: { "userId": "..." }
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Map<String, String>> cancelOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String orderId,
            @RequestBody Map<String, String> body) {

        SessionToken sessionToken = toSessionToken(authHeader);
        String userId = body.get("userId");
        try {
            activeOrderService.cancelActiveOrder(sessionToken, userId, orderId);
            return ResponseEntity.ok(Map.of("message", "Order cancelled successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/orders/{orderId}/seats
    // Reserves additional seats and adds them to the active order.
    // Body: { "seatIds": ["A-1", "A-2"] }
    @PostMapping("/{orderId}/seats")
    public ResponseEntity<Map<String, String>> addSeats(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String orderId,
            @RequestBody AddSeatsRequestDTO body) {

        SessionToken sessionToken = toSessionToken(authHeader);
        try {
            activeOrderService.addSeatsToActiveOrder(sessionToken, orderId, body.getSeatIds());
            return ResponseEntity.ok(Map.of("message", "Seats added successfully."));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/orders/{orderId}/standing
    // Reserves standing-area tickets and adds them to the active order.
    // Body: { "areaId": "GA-1", "quantity": 3 }
    @PostMapping("/{orderId}/standing")
    public ResponseEntity<Map<String, String>> addStandingArea(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String orderId,
            @RequestBody AddStandingAreaRequestDTO body) {

        SessionToken sessionToken = toSessionToken(authHeader);
        try {
            activeOrderService.addStandingAreaToActiveOrder(
                    sessionToken, orderId, body.getAreaId(), body.getQuantity());
            return ResponseEntity.ok(Map.of("message", "Standing area tickets added successfully."));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/orders/{orderId}
    // Replaces the full ticket selection in the order (diff is computed internally).
    // Body: ActiveOrderDTO
    @PutMapping("/{orderId}")
    public ResponseEntity<Map<String, String>> updateOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String orderId,
            @RequestBody ActiveOrderDTO body) {

        SessionToken sessionToken = toSessionToken(authHeader);
        try {
            activeOrderService.updateActiveOrder(sessionToken, body);
            return ResponseEntity.ok(Map.of("message", "Order updated successfully."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/orders/{orderId}/checkout
    // Completes the order: checks purchase policy, charges payment, issues barcodes.
    // Body: { "amount": 99.99 }
    // Returns 200: { "barcodes": ["order-1-A1", "order-1-GA-0", ...] }
    @PostMapping("/{orderId}/checkout")
    public ResponseEntity<?> checkout(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String orderId,
            @RequestBody CheckoutRequestDTO body) {

        SessionToken sessionToken = toSessionToken(authHeader);
        try {
            PaymentDetailsDTO paymentDetails = new PaymentDetailsDTO(
                    body.getCreditCardNumber(),
                    body.getCardHolderName(),
                    body.getExpirationDate(),
                    body.getCvv()
            );
            List<BarcodeDTO> barcodes = activeOrderService.completeOrder(
                    paymentGateway, sessionToken, body.toPaymentDetails(), orderId);
            List<String> barcodeValues = barcodes.stream()
                    .map(BarcodeDTO::getBarcodeValue)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("barcodes", barcodeValues));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }
    /**
     * GET /api/active-orders/user/{userId}
     * Fetches the single active, unexpired ticket reservation envelope for a specific user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ActiveOrderDTO> getActiveOrderByUserId(
            @RequestHeader("Authorization") String token,
            @PathVariable("userId") String userId) {
                try{
                    SessionToken sessionToken = toSessionToken(token);

                ActiveOrderDTO activeOrderDTO = activeOrderService.getActiveOrderByUserId(sessionToken, userId);
                // 3. Return 200 OK with the payload, or let your global exception handler catch a 404
                if (activeOrderDTO == null) {
                    return ResponseEntity.notFound().build();
                }
                return ResponseEntity.ok(activeOrderDTO);
                }
                catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
}

    private SessionToken toSessionToken(String authHeader) {
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : authHeader;
        return new SessionToken(token, Long.MAX_VALUE);
    }
}
