package com.ticketpurchasingsystem.project.Controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticketpurchasingsystem.project.application.ISystemAdminService;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;
import com.ticketpurchasingsystem.project.domain.User.UserDTO;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ISystemAdminService adminService;

    public AdminController(ISystemAdminService adminService) {
        this.adminService = adminService;
    }

    // GET /api/admin/active-orders
    @GetMapping("/active-orders")
    public ResponseEntity<?> getActiveOrders(@RequestHeader("Authorization") String authHeader) {
        try {
            List<ActiveOrderDTO> orders = adminService.getAllActiveOrders(extractToken(authHeader));
            return ResponseEntity.ok(orders);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/admin/history-orders
    @GetMapping("/history-orders")
    public ResponseEntity<?> getHistoryOrders(@RequestHeader("Authorization") String authHeader) {
        try {
            List<HistoryOrderDTO> orders = adminService.getAllHistoryOrders(extractToken(authHeader));
            return ResponseEntity.ok(orders);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/admin/users
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getUsers(@RequestHeader("Authorization") String authHeader) {
        try {
            List<UserDTO> users = adminService.getAllUsers(extractToken(authHeader));
            return ResponseEntity.ok(users);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    private String extractToken(String authHeader) {
        return (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : authHeader;
    }
}
