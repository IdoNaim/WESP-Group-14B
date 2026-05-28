package com.ticketpurchasingsystem.project.Controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ticketpurchasingsystem.project.application.ForbiddenException;
import com.ticketpurchasingsystem.project.application.INotificationService;
import com.ticketpurchasingsystem.project.application.NotFoundException;
import com.ticketpurchasingsystem.project.application.UnauthorizedException;
import com.ticketpurchasingsystem.project.domain.Utils.NotificationDTO;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private INotificationService notificationService;

    private ObjectMapper objectMapper;
    private static final String TOKEN = "Bearer valid-token";
    private static final String RAW_TOKEN = "valid-token";
    private static final String NOTIF_ID = "NOTIF-1";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private NotificationDTO sampleDTO() {
        return new NotificationDTO(NOTIF_ID, "alice", "Concert at 8pm", false, LocalDateTime.now());
    }

    // ── POST /api/notifications ─────────────────────────────────────────────

    @Test
    void GivenValidRequest_WhenCreateNotification_ThenReturn201() throws Exception {
        when(notificationService.createNotification(any(), any(), any())).thenReturn(sampleDTO());

        String body = "{\"targetUserId\":\"alice\",\"message\":\"Concert at 8pm\"}";

        mockMvc.perform(post("/api/notifications")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(NOTIF_ID))
                .andExpect(jsonPath("$.userId").value("alice"))
                .andExpect(jsonPath("$.read").value(false));
    }

    @Test
    void GivenNonAdminCaller_WhenCreateNotification_ThenReturn403() throws Exception {
        when(notificationService.createNotification(any(), any(), any()))
                .thenThrow(new ForbiddenException("Only administrators can send targeted notifications"));

        String body = "{\"targetUserId\":\"alice\",\"message\":\"hi\"}";

        mockMvc.perform(post("/api/notifications")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void GivenInvalidToken_WhenCreateNotification_ThenReturn401() throws Exception {
        when(notificationService.createNotification(any(), any(), any()))
                .thenThrow(new UnauthorizedException("Invalid session token"));

        String body = "{\"targetUserId\":\"alice\",\"message\":\"hi\"}";

        mockMvc.perform(post("/api/notifications")
                        .header("Authorization", "bad-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/notifications ──────────────────────────────────────────────

    @Test
    void GivenValidToken_WhenGetNotifications_ThenReturn200WithList() throws Exception {
        when(notificationService.getNotificationsForUser(any()))
                .thenReturn(List.of(sampleDTO()));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(NOTIF_ID))
                .andExpect(jsonPath("$[0].message").value("Concert at 8pm"));
    }

    @Test
    void GivenInvalidToken_WhenGetNotifications_ThenReturn401() throws Exception {
        when(notificationService.getNotificationsForUser(any()))
                .thenThrow(new UnauthorizedException("Invalid session token"));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "bad-token"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/notifications/unread-count ────────────────────────────────

    @Test
    void GivenValidToken_WhenGetUnreadCount_ThenReturn200WithCount() throws Exception {
        when(notificationService.getUnreadCount(any())).thenReturn(3L);

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));
    }

    @Test
    void GivenInvalidToken_WhenGetUnreadCount_ThenReturn401() throws Exception {
        when(notificationService.getUnreadCount(any()))
                .thenThrow(new UnauthorizedException("Invalid session token"));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "bad-token"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/notifications/{id} ─────────────────────────────────────────

    @Test
    void GivenOwner_WhenGetById_ThenReturn200() throws Exception {
        when(notificationService.getNotificationById(eq(RAW_TOKEN), eq(NOTIF_ID)))
                .thenReturn(sampleDTO());

        mockMvc.perform(get("/api/notifications/" + NOTIF_ID)
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(NOTIF_ID));
    }

    @Test
    void GivenInvalidToken_WhenGetById_ThenReturn401() throws Exception {
        when(notificationService.getNotificationById(any(), any()))
                .thenThrow(new UnauthorizedException("Invalid session token"));

        mockMvc.perform(get("/api/notifications/" + NOTIF_ID)
                        .header("Authorization", "bad-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GivenNotFound_WhenGetById_ThenReturn404() throws Exception {
        when(notificationService.getNotificationById(any(), any()))
                .thenThrow(new NotFoundException("Notification not found"));

        mockMvc.perform(get("/api/notifications/NOTIF-X")
                        .header("Authorization", TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    void GivenWrongUser_WhenGetById_ThenReturn403() throws Exception {
        when(notificationService.getNotificationById(any(), any()))
                .thenThrow(new ForbiddenException("Access denied: notification belongs to another user"));

        mockMvc.perform(get("/api/notifications/" + NOTIF_ID)
                        .header("Authorization", "other-token"))
                .andExpect(status().isForbidden());
    }

    // ── POST /api/notifications/event/{eventId} ─────────────────────────────

    @Test
    void GivenValidRequest_WhenNotifyEvent_ThenReturn201() throws Exception {
        when(notificationService.createNotificationsForEvent(any(), any(), any()))
                .thenReturn(List.of(sampleDTO()));

        mockMvc.perform(post("/api/notifications/event/EVT-1")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Event updated\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value(NOTIF_ID));
    }

    @Test
    void GivenUnauthorizedCaller_WhenNotifyEvent_ThenReturn403() throws Exception {
        when(notificationService.createNotificationsForEvent(any(), any(), any()))
                .thenThrow(new ForbiddenException("Caller is not an owner or manager of this production company"));

        mockMvc.perform(post("/api/notifications/event/EVT-1")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Event updated\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void GivenInvalidToken_WhenNotifyEvent_ThenReturn401() throws Exception {
        when(notificationService.createNotificationsForEvent(any(), any(), any()))
                .thenThrow(new UnauthorizedException("Invalid session token"));

        mockMvc.perform(post("/api/notifications/event/EVT-1")
                        .header("Authorization", "bad-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Event updated\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/notifications/production/{companyId} ──────────────────────

    @Test
    void GivenValidRequest_WhenNotifyProduction_ThenReturn201() throws Exception {
        when(notificationService.createNotificationsForProduction(any(), eq(42), any()))
                .thenReturn(List.of(sampleDTO()));

        mockMvc.perform(post("/api/notifications/production/42")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Team update\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value(NOTIF_ID));
    }

    @Test
    void GivenUnauthorizedCaller_WhenNotifyProduction_ThenReturn403() throws Exception {
        when(notificationService.createNotificationsForProduction(any(), anyInt(), any()))
                .thenThrow(new ForbiddenException("Caller is not an owner or manager of this production company"));

        mockMvc.perform(post("/api/notifications/production/42")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Team update\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void GivenInvalidToken_WhenNotifyProduction_ThenReturn401() throws Exception {
        when(notificationService.createNotificationsForProduction(any(), anyInt(), any()))
                .thenThrow(new UnauthorizedException("Invalid session token"));

        mockMvc.perform(post("/api/notifications/production/42")
                        .header("Authorization", "bad-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Team update\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /api/notifications/{id}/read ────────────────────────────────────

    @Test
    void GivenOwner_WhenMarkAsRead_ThenReturn200() throws Exception {
        when(notificationService.markAsRead(any(), eq(NOTIF_ID))).thenReturn(true);

        mockMvc.perform(put("/api/notifications/" + NOTIF_ID + "/read")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void GivenInvalidToken_WhenMarkAsRead_ThenReturn401() throws Exception {
        when(notificationService.markAsRead(any(), any()))
                .thenThrow(new UnauthorizedException("Invalid session token"));

        mockMvc.perform(put("/api/notifications/" + NOTIF_ID + "/read")
                        .header("Authorization", "bad-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GivenWrongUser_WhenMarkAsRead_ThenReturn403() throws Exception {
        when(notificationService.markAsRead(any(), any()))
                .thenThrow(new ForbiddenException("Access denied: notification belongs to another user"));

        mockMvc.perform(put("/api/notifications/" + NOTIF_ID + "/read")
                        .header("Authorization", "other-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void GivenNotFound_WhenMarkAsRead_ThenReturn404() throws Exception {
        when(notificationService.markAsRead(any(), any()))
                .thenThrow(new NotFoundException("Notification not found"));

        mockMvc.perform(put("/api/notifications/NOTIF-X/read")
                        .header("Authorization", TOKEN))
                .andExpect(status().isNotFound());
    }
}
