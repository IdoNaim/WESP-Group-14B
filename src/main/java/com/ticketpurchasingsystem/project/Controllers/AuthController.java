package com.ticketpurchasingsystem.project.Controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticketpurchasingsystem.project.Controllers.apidto.LoginRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.RegisterRequestDTO;
import com.ticketpurchasingsystem.project.application.UserService.IUserService;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;

@RestController
@RequestMapping("/api/identity")
public class AuthController {

    private final IUserService userService;

    public AuthController(IUserService userService) {
        this.userService = userService;
    }

    // POST /api/identity/guest
    // Must be called first to obtain a session token before login or register.
    // Returns: { "token": "<guestToken>" }
    @PostMapping("/guest")
    public ResponseEntity<Map<String, String>> guestEntry() {
        try {
            String guestToken = userService.guestEntry();
            return ResponseEntity.ok(Map.of("token", guestToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/identity/register
    // Header: Authorization: Bearer <guestToken>
    // Body: { "userId", "name", "password", "email", "userGroupDiscount" }
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody RegisterRequestDTO body) {

        String token = extractToken(authHeader);
        try {
            UserGroupDiscount discount = body.getUserGroupDiscount() != null
                    ? body.getUserGroupDiscount()
                    : UserGroupDiscount.NONE;

            userService.registerUser(
                    body.getUserId(),
                    body.getName(),
                    body.getPassword(),
                    body.getEmail(),
                    discount,
                    token);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "User registered successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/identity/login
    // Header: Authorization: Bearer <currentToken>  (guest or any valid token)
    // Body: { "userId", "password" }
    // Returns: { "token": "<newSessionToken>", "userId": "<userId>" }
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody LoginRequestDTO body) {

        String token = extractToken(authHeader);
        try {
            String newToken = userService.loginUser(body.getUserId(), body.getPassword(), token);
            return ResponseEntity.ok(Map.of(
                    "token", newToken,
                    "userId", body.getUserId()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/identity/logout
    // Header: Authorization: Bearer <userToken>
    // Body: { "userId" }
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {

        String token = extractToken(authHeader);
        String userId = body.get("userId");
        try {
            userService.logoutUser(userId, token);
            return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/identity/exit
    // Header: Authorization: Bearer <token>
    // Fully exits the platform (removes guest or logs out registered user).
    @PostMapping("/exit")
    public ResponseEntity<Map<String, String>> exit(
            @RequestHeader("Authorization") String authHeader) {

        String token = extractToken(authHeader);
        try {
            userService.Exit(token);
            return ResponseEntity.ok(Map.of("message", "Exited platform successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
}
