package com.ticketpurchasingsystem.project.Controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.ticketpurchasingsystem.project.application.ISystemAdminService;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ISystemAdminService adminService;

    private static final String TOKEN = "Bearer valid-token";

    private ActiveOrderDTO sampleActiveOrder() {
        return new ActiveOrderDTO("order-1", "user-1", "event-1",
                Timestamp.from(Instant.now()), List.of("A1"), new HashMap<>());
    }

    private HistoryOrderDTO sampleHistoryOrder() {
        return new HistoryOrderDTO("order-2", "user-1", "event-1",
                42, Timestamp.from(Instant.now()), 99.0, List.of("A2"), new HashMap<>());
    }

    private UserInfo sampleUser() {
        return new UserInfo("user-1", "alice", "alice@example.com", "", UserGroupDiscount.NONE);
    }

    // ── GET /api/admin/active-orders ─────────────────────────────────────────

    @Test
    void GivenAdminToken_WhenGetActiveOrders_ThenReturn200WithList() throws Exception {
        when(adminService.getAllActiveOrders("valid-token")).thenReturn(List.of(sampleActiveOrder()));

        mockMvc.perform(get("/api/admin/active-orders")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("order-1"))
                .andExpect(jsonPath("$[0].userId").value("user-1"));
    }

    @Test
    void GivenInvalidToken_WhenGetActiveOrders_ThenReturn401() throws Exception {
        when(adminService.getAllActiveOrders(any())).thenThrow(new RuntimeException("Invalid session token"));

        mockMvc.perform(get("/api/admin/active-orders")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid session token"));
    }

    @Test
    void GivenNonAdminToken_WhenGetActiveOrders_ThenReturn401() throws Exception {
        when(adminService.getAllActiveOrders(any())).thenThrow(new RuntimeException("User is not an admin"));

        mockMvc.perform(get("/api/admin/active-orders")
                        .header("Authorization", TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("User is not an admin"));
    }

    @Test
    void GivenAdminToken_WhenGetActiveOrders_ThenReturnEmptyList() throws Exception {
        when(adminService.getAllActiveOrders("valid-token")).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/active-orders")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/admin/history-orders ────────────────────────────────────────

    @Test
    void GivenAdminToken_WhenGetHistoryOrders_ThenReturn200WithList() throws Exception {
        when(adminService.getAllHistoryOrders("valid-token")).thenReturn(List.of(sampleHistoryOrder()));

        mockMvc.perform(get("/api/admin/history-orders")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("order-2"))
                .andExpect(jsonPath("$[0].userId").value("user-1"));
    }

    @Test
    void GivenInvalidToken_WhenGetHistoryOrders_ThenReturn401() throws Exception {
        when(adminService.getAllHistoryOrders(any())).thenThrow(new RuntimeException("Invalid session token"));

        mockMvc.perform(get("/api/admin/history-orders")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid session token"));
    }

    @Test
    void GivenNonAdminToken_WhenGetHistoryOrders_ThenReturn401() throws Exception {
        when(adminService.getAllHistoryOrders(any())).thenThrow(new RuntimeException("User is not an admin"));

        mockMvc.perform(get("/api/admin/history-orders")
                        .header("Authorization", TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("User is not an admin"));
    }

    @Test
    void GivenAdminToken_WhenGetHistoryOrders_ThenReturnEmptyList() throws Exception {
        when(adminService.getAllHistoryOrders("valid-token")).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/history-orders")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/admin/users ─────────────────────────────────────────────────

    @Test
    void GivenRequest_WhenGetUsers_ThenReturn200WithList() throws Exception {
        when(adminService.getAllUsers()).thenReturn(List.of(sampleUser()));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("user-1"))
                .andExpect(jsonPath("$[0].email").value("alice@example.com"));
    }

    @Test
    void GivenRequest_WhenGetUsers_ThenReturnEmptyList() throws Exception {
        when(adminService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
