package com.ticketpurchasingsystem.project.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ticketpurchasingsystem.project.domain.Utils.NotificationDTO;
import com.ticketpurchasingsystem.project.domain.notification.INotificationRepo;
import com.ticketpurchasingsystem.project.domain.notification.Notification;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private INotificationRepo notificationRepo;
    @Mock private AuthenticationService authenticationService;
    @Mock private com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo historyOrderRepo;
    @Mock private com.ticketpurchasingsystem.project.domain.Production.IProdRepo prodRepo;

    private NotificationService notificationService;

    private static final String VALID_TOKEN   = "valid-token";
    private static final String INVALID_TOKEN = "bad-token";
    private static final String USER_ID       = "user-001";
    private static final String OTHER_USER_ID = "user-002";
    private static final String MESSAGE       = "Your ticket is ready";

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepo, authenticationService, historyOrderRepo, prodRepo);
    }

    // ── createNotification ──────────────────────────────────────────────────

    @Test
    void GivenValidToken_WhenCreateNotification_ThenReturnDTO() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);

        NotificationDTO result = notificationService.createNotification(VALID_TOKEN, USER_ID, MESSAGE);

        assertNotNull(result);
        assertEquals(USER_ID, result.getUserId());
        assertEquals(MESSAGE, result.getMessage());
        assertFalse(result.isRead());
        verify(notificationRepo, times(1)).save(any(Notification.class));
    }

    @Test
    void GivenInvalidToken_WhenCreateNotification_ThenThrow() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.createNotification(INVALID_TOKEN, USER_ID, MESSAGE));
        assertNotNull(ex.getMessage());
        verify(notificationRepo, never()).save(any());
    }

    @Test
    void GivenBlankTargetUserId_WhenCreateNotification_ThenThrow() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.createNotification(VALID_TOKEN, "  ", MESSAGE));
        assertNotNull(ex.getMessage());
        verify(notificationRepo, never()).save(any());
    }

    @Test
    void GivenBlankMessage_WhenCreateNotification_ThenThrow() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.createNotification(VALID_TOKEN, USER_ID, ""));
        assertNotNull(ex.getMessage());
        verify(notificationRepo, never()).save(any());
    }

    // ── getNotificationsForUser ─────────────────────────────────────────────

    @Test
    void GivenValidToken_WhenGetNotifications_ThenReturnUserNotifications() {
        Notification n = new Notification("NOTIF-1", USER_ID, MESSAGE);
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(notificationRepo.findByUserId(USER_ID)).thenReturn(List.of(n));

        List<NotificationDTO> result = notificationService.getNotificationsForUser(VALID_TOKEN);

        assertEquals(1, result.size());
        assertEquals(USER_ID, result.get(0).getUserId());
    }

    @Test
    void GivenInvalidToken_WhenGetNotifications_ThenThrow() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.getNotificationsForUser(INVALID_TOKEN));
        assertNotNull(ex.getMessage());
    }

    @Test
    void GivenNoNotifications_WhenGetNotifications_ThenReturnEmpty() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(notificationRepo.findByUserId(USER_ID)).thenReturn(List.of());

        List<NotificationDTO> result = notificationService.getNotificationsForUser(VALID_TOKEN);

        assertTrue(result.isEmpty());
    }

    // ── getNotificationById ─────────────────────────────────────────────────

    @Test
    void GivenOwnerToken_WhenGetById_ThenReturnDTO() {
        Notification n = new Notification("NOTIF-1", USER_ID, MESSAGE);
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(notificationRepo.findById("NOTIF-1")).thenReturn(n);

        NotificationDTO result = notificationService.getNotificationById(VALID_TOKEN, "NOTIF-1");

        assertNotNull(result);
        assertEquals("NOTIF-1", result.getId());
    }

    @Test
    void GivenNotificationNotFound_WhenGetById_ThenThrow() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(notificationRepo.findById("NOTIF-X")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.getNotificationById(VALID_TOKEN, "NOTIF-X"));
        assertNotNull(ex.getMessage());
    }

    @Test
    void GivenDifferentUser_WhenGetById_ThenThrowAccessDenied() {
        Notification n = new Notification("NOTIF-1", OTHER_USER_ID, MESSAGE);
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(notificationRepo.findById("NOTIF-1")).thenReturn(n);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.getNotificationById(VALID_TOKEN, "NOTIF-1"));
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    // ── markAsRead ──────────────────────────────────────────────────────────

    @Test
    void GivenOwnerToken_WhenMarkAsRead_ThenReturnTrueAndSetRead() {
        Notification n = new Notification("NOTIF-1", USER_ID, MESSAGE);
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(notificationRepo.findById("NOTIF-1")).thenReturn(n);

        boolean result = notificationService.markAsRead(VALID_TOKEN, "NOTIF-1");

        assertTrue(result);
        assertTrue(n.isRead());
    }

    @Test
    void GivenDifferentUser_WhenMarkAsRead_ThenThrowAndLeaveUnread() {
        Notification n = new Notification("NOTIF-1", OTHER_USER_ID, MESSAGE);
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(notificationRepo.findById("NOTIF-1")).thenReturn(n);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.markAsRead(VALID_TOKEN, "NOTIF-1"));
        assertTrue(ex.getMessage().contains("Access denied"));
        assertFalse(n.isRead());
    }

    @Test
    void GivenNotificationNotFound_WhenMarkAsRead_ThenThrow() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(notificationRepo.findById("NOTIF-X")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.markAsRead(VALID_TOKEN, "NOTIF-X"));
        assertNotNull(ex.getMessage());
    }

    @Test
    void GivenInvalidToken_WhenMarkAsRead_ThenThrow() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.markAsRead(INVALID_TOKEN, "NOTIF-1"));
        assertNotNull(ex.getMessage());
    }

    // ── getUnreadCount ──────────────────────────────────────────────────────

    @Test
    void GivenValidToken_WhenGetUnreadCount_ThenReturnCount() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(notificationRepo.countUnreadByUserId(USER_ID)).thenReturn(3L);

        assertEquals(3L, notificationService.getUnreadCount(VALID_TOKEN));
    }

    @Test
    void GivenInvalidToken_WhenGetUnreadCount_ThenThrow() {
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.getUnreadCount(INVALID_TOKEN));
        assertNotNull(ex.getMessage());
    }

    @Test
    void GivenNoUnread_WhenGetUnreadCount_ThenReturnZero() {
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(notificationRepo.countUnreadByUserId(USER_ID)).thenReturn(0L);

        assertEquals(0L, notificationService.getUnreadCount(VALID_TOKEN));
    }

    // ── createSystemNotification ────────────────────────────────────────────

    @Test
    void GivenValidArgs_WhenCreateSystemNotification_ThenReturnDTO() {
        NotificationDTO result = notificationService.createSystemNotification(USER_ID, MESSAGE);

        assertNotNull(result);
        assertEquals(USER_ID, result.getUserId());
        assertEquals(MESSAGE, result.getMessage());
        assertFalse(result.isRead());
        verify(notificationRepo, times(1)).save(any(Notification.class));
    }

    @Test
    void GivenBlankUserId_WhenCreateSystemNotification_ThenThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.createSystemNotification("  ", MESSAGE));
        assertNotNull(ex.getMessage());
        verify(notificationRepo, never()).save(any());
    }

    @Test
    void GivenNullUserId_WhenCreateSystemNotification_ThenThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.createSystemNotification(null, MESSAGE));
        assertNotNull(ex.getMessage());
        verify(notificationRepo, never()).save(any());
    }

    @Test
    void GivenBlankMessage_WhenCreateSystemNotification_ThenThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.createSystemNotification(USER_ID, ""));
        assertNotNull(ex.getMessage());
        verify(notificationRepo, never()).save(any());
    }

    @Test
    void GivenValidArgs_WhenCreateSystemNotification_ThenNoTokenRequired() {
        // System notifications bypass auth — no stubbing of authenticationService needed
        NotificationDTO result = notificationService.createSystemNotification(USER_ID, MESSAGE);

        assertNotNull(result);
        verify(authenticationService, never()).validate(any());
    }
}
