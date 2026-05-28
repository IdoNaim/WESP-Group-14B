package com.ticketpurchasingsystem.project.acceptance.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.NotificationService;
import com.ticketpurchasingsystem.project.domain.Utils.NotificationDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.infrastructure.HistoryOrderRepo;
import com.ticketpurchasingsystem.project.infrastructure.InMemoryNotificationRepo;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import com.ticketpurchasingsystem.project.infrastructure.ProdRepo;

class NotificationAcceptanceTest {

    private NotificationService notificationService;
    private String userToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() {
        InMemorySessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        ReflectionTestUtils.setField(domainAuthService, "secret", "aSecureTestSecretKeyMustBe32Bytes");
        domainAuthService.init();
        AuthenticationService authService = new AuthenticationService(domainAuthService, sessionRepo);

        userToken      = authService.login("alice");
        otherUserToken = authService.login("bob");

        notificationService = new NotificationService(new InMemoryNotificationRepo(), authService, HistoryOrderRepo.getInstance(), new ProdRepo());
    }

    // ── create ──────────────────────────────────────────────────────────────

    @Test
    void GivenValidRequest_WhenCreateNotification_ThenNotificationExists() {
        String aliceId = "alice";

        NotificationDTO created = notificationService.createNotification(userToken, aliceId, "Concert starts at 8pm");

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(aliceId, created.getUserId());
        assertEquals("Concert starts at 8pm", created.getMessage());
        assertFalse(created.isRead());
        assertNotNull(created.getCreatedAt());
    }

    @Test
    void GivenInvalidToken_WhenCreateNotification_ThenThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.createNotification("bad-token", "alice", "hello"));
        assertNotNull(ex.getMessage());
    }

    // ── get own notifications ───────────────────────────────────────────────

    @Test
    void GivenNotificationsExist_WhenGetForUser_ThenReturnOnlyOwnNotifications() {
        notificationService.createNotification(userToken, "alice", "msg-1");
        notificationService.createNotification(userToken, "alice", "msg-2");
        notificationService.createNotification(userToken, "bob",   "bob-msg");

        List<NotificationDTO> aliceNotifs = notificationService.getNotificationsForUser(userToken);

        assertEquals(2, aliceNotifs.size());
        assertTrue(aliceNotifs.stream().allMatch(n -> "alice".equals(n.getUserId())));
    }

    @Test
    void GivenNoNotifications_WhenGetForUser_ThenReturnEmpty() {
        List<NotificationDTO> result = notificationService.getNotificationsForUser(userToken);
        assertTrue(result.isEmpty());
    }

    @Test
    void GivenInvalidToken_WhenGetForUser_ThenThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.getNotificationsForUser("expired-token"));
        assertNotNull(ex.getMessage());
    }

    // ── get by id ───────────────────────────────────────────────────────────

    @Test
    void GivenOwner_WhenGetById_ThenReturnNotification() {
        NotificationDTO created = notificationService.createNotification(userToken, "alice", "hello");

        NotificationDTO fetched = notificationService.getNotificationById(userToken, created.getId());

        assertNotNull(fetched);
        assertEquals(created.getId(), fetched.getId());
        assertEquals("hello", fetched.getMessage());
    }

    @Test
    void GivenWrongUser_WhenGetById_ThenThrowAccessDenied() {
        NotificationDTO created = notificationService.createNotification(userToken, "alice", "private");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.getNotificationById(otherUserToken, created.getId()));
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    void GivenUnknownId_WhenGetById_ThenThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.getNotificationById(userToken, "NOTIF-DOES-NOT-EXIST"));
        assertNotNull(ex.getMessage());
    }

    // ── mark as read ────────────────────────────────────────────────────────

    @Test
    void GivenUnreadNotification_WhenMarkAsRead_ThenIsReadBecomesTrue() {
        NotificationDTO created = notificationService.createNotification(userToken, "alice", "check this");
        assertFalse(created.isRead());

        boolean result = notificationService.markAsRead(userToken, created.getId());

        assertTrue(result);
        NotificationDTO fetched = notificationService.getNotificationById(userToken, created.getId());
        assertTrue(fetched.isRead());
    }

    @Test
    void GivenAlreadyRead_WhenMarkAsReadAgain_ThenStillTrue() {
        NotificationDTO created = notificationService.createNotification(userToken, "alice", "check this");
        notificationService.markAsRead(userToken, created.getId());

        boolean result = notificationService.markAsRead(userToken, created.getId());

        assertTrue(result);
    }

    @Test
    void GivenWrongUser_WhenMarkAsRead_ThenThrowAndStayUnread() {
        NotificationDTO created = notificationService.createNotification(userToken, "alice", "private");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.markAsRead(otherUserToken, created.getId()));
        assertTrue(ex.getMessage().contains("Access denied"));

        NotificationDTO fetched = notificationService.getNotificationById(userToken, created.getId());
        assertFalse(fetched.isRead());
    }

    // ── unread count ────────────────────────────────────────────────────────

    @Test
    void GivenMultipleNotifications_WhenGetUnreadCount_ThenOnlyCountUnread() {
        String aliceId = "alice";
        NotificationDTO n1 = notificationService.createNotification(userToken, aliceId, "msg-1");
        notificationService.createNotification(userToken, aliceId, "msg-2");
        notificationService.createNotification(userToken, aliceId, "msg-3");

        notificationService.markAsRead(userToken, n1.getId());

        long unread = notificationService.getUnreadCount(userToken);
        assertEquals(2, unread);
    }

    @Test
    void GivenNoNotifications_WhenGetUnreadCount_ThenReturnZero() {
        assertEquals(0, notificationService.getUnreadCount(userToken));
    }

    @Test
    void GivenInvalidToken_WhenGetUnreadCount_ThenThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                notificationService.getUnreadCount("bad-token"));
        assertNotNull(ex.getMessage());
    }
}
