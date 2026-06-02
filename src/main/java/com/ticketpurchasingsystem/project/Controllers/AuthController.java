package com.ticketpurchasingsystem.project.Controllers;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticketpurchasingsystem.project.Controllers.apidto.LoginRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.ProfileUpdateRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.RegisterRequestDTO;
import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.UserService.IUserService;
import com.ticketpurchasingsystem.project.domain.User.UserDTO;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import com.ticketpurchasingsystem.project.domain.systemAdmin.IAdminRepo;

import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/api/identity")
public class AuthController {

    private final IUserService userService;
    private final AuthenticationService authenticationService;
    private final IAdminRepo adminRepo;

    public AuthController(IUserService userService, AuthenticationService authenticationService, IAdminRepo adminRepo) {
        this.userService = userService;
        this.authenticationService = authenticationService;
        this.adminRepo = adminRepo;
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
            System.out.println("--- LOGIN ATTEMPT ---");
            System.out.println("User ID: " + body.getUserId());
            System.out.println("Password: " + body.getPassword());
            System.out.println("Guest Token: " + token);
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
            System.out.println("--- LOGIN FAILED ---");
            e.printStackTrace();
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
            String newGuestToken = userService.logoutUser(userId, token);
            return ResponseEntity.ok(Map.of("message", "Logged out successfully.", "token", newGuestToken));
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

    // GET /api/identity/me
    // Header: Authorization: Bearer <token>
    // Returns current user details (excluding password)
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        try {
            if (!authenticationService.validate(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired session token."));
            }
            String userId = authenticationService.getUser(token);
            UserDTO userDTO = userService.getUser(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userDTO.getUserId());
            response.put("name", userDTO.getUsername());
            response.put("email", userDTO.getEmail());
            response.put("userGroupDiscount", userDTO.getGroupDiscount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/identity/profile
    // Header: Authorization: Bearer <token>
    // Body: { "name", "email", "userGroupDiscount" }
    // Updates user profile details securely.
    @PutMapping("/profile")
    public ResponseEntity<Map<String, String>> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ProfileUpdateRequestDTO body) {

        String token = extractToken(authHeader);
        try {
            if (!authenticationService.validate(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired session token."));
            }
            String userId = authenticationService.getUser(token);
            UserDTO userDTO = userService.getUser(userId);

            if (body.getName() != null && !body.getName().equals(userDTO.getUsername())) {
                userService.editUsername(userId, userDTO.getUsername(), body.getName(), token);
            }
            if (body.getEmail() != null && !body.getEmail().equals(userDTO.getEmail())) {
                userService.editEmail(userId, userDTO.getEmail(), body.getEmail(), token);
            }
            if (body.getUserGroupDiscount() != null && body.getUserGroupDiscount() != userDTO.getGroupDiscount()) {
                userService.setUserGroupDiscount(userId, body.getUserGroupDiscount(), token);
            }

            return ResponseEntity.ok(Map.of("message", "Profile updated successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/identity/permissions
    // Header: Authorization: Bearer <token>
    // Returns current user's state, admin role, and production roles.
    @GetMapping("/permissions")
    public ResponseEntity<Map<String, Object>> getPermissions(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        try {
            if (!authenticationService.validate(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired session token."));
            }
            String userId = authenticationService.getUser(token);
            UserInfo userInfo = userService.getUserInfo(userId);

            boolean isAdmin = adminRepo.isAdmin(userId);
            String state = userInfo.getUserState().name();

            Map<Integer, String> productionRoles = Collections.emptyMap();
            if (userInfo.getUserProduction() != null) {
                productionRoles = userInfo.getUserProduction().getAllProductions().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().name()
                        ));
            }

            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "state", state,
                    "isAdmin", isAdmin,
                    "productionRoles", productionRoles
            ));
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
