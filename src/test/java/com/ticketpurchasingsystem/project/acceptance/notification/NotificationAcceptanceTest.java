package com.ticketpurchasingsystem.project.acceptance.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.domain.exceptions.ForbiddenException;
import com.ticketpurchasingsystem.project.domain.exceptions.NotFoundException;
import com.ticketpurchasingsystem.project.application.NotificationService;
import com.ticketpurchasingsystem.project.application.UnauthorizedException;
import com.ticketpurchasingsystem.project.domain.Utils.NotificationDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.infrastructure.EventRepo;
import com.ticketpurchasingsystem.project.infrastructure.HistoryOrderRepo;
import com.ticketpurchasingsystem.project.infrastructure.InMemoryNotificationRepo;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import com.ticketpurchasingsystem.project.infrastructure.ProdRepo;

class NotificationAcceptanceTest {

    private NotificationService notificationService;
    private String adminToken;
    private String userToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() {
        InMemorySessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        ReflectionTestUtils.setField(domainAuthService, "secret", "aSecureTestSecretKeyMustBe32Bytes");
        domainAuthService.init();
        AuthenticationService authService = new AuthenticationService(domainAuthService, sessionRepo);

        adminToken     = authService.login("admin", "admin");
        userToken      = authService.login("alice");
        otherUserToken = authService.login("bob");

        notificationService = new NotificationService(
                new InMemoryNotificationRepo(), authService,
                HistoryOrderRepo.getInstance(), new ProdRepo(), EventRepo.getInstance());
    }

    // ── create ──────────────────────────────────────────────────────────────

    @Test
    void GivenAdminToken_WhenCreateNotification_ThenNotificationExists() {
        String aliceId = "alice";

        NotificationDTO created = notificationService.createNotification(adminToken, aliceId, "Concert starts at 8pm");

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(aliceId, created.getUserId());
        assertEquals("Concert starts at 8pm", created.getMessage());
        assertFalse(created.isRead());
        assertNotNull(created.getCreatedAt());
    }

    @Test
    void GivenNonAdminToken_WhenCreateNotification_ThenThrowForbidden() {
        assertThrows(ForbiddenException.class, () ->
                notificationService.createNotification(userToken, "alice", "hello"));
    }

    @Test
    void GivenInvalidToken_WhenCreateNotification_ThenThrow() {
        assertThrows(UnauthorizedException.class, () ->
                notificationService.createNotification("bad-token", "alice", "hello"));
    }

    // ── get own notifications ───────────────────────────────────────────────

    @Test
    void GivenNotificationsExist_WhenGetForUser_ThenReturnOnlyOwnNotifications() {
        notificationService.createNotification(adminToken, "alice", "msg-1");
        notificationService.createNotification(adminToken, "alice", "msg-2");
        notificationService.createNotification(adminToken, "bob",   "bob-msg");

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
        assertThrows(UnauthorizedException.class, () ->
                notificationService.getNotificationsForUser("expired-token"));
    }

    // ── get by id ───────────────────────────────────────────────────────────

    @Test
    void GivenOwner_WhenGetById_ThenReturnNotification() {
        NotificationDTO created = notificationService.createNotification(adminToken, "alice", "hello");

        NotificationDTO fetched = notificationService.getNotificationById(userToken, created.getId());

        assertNotNull(fetched);
        assertEquals(created.getId(), fetched.getId());
        assertEquals("hello", fetched.getMessage());
    }

    @Test
    void GivenWrongUser_WhenGetById_ThenThrowAccessDenied() {
        NotificationDTO created = notificationService.createNotification(adminToken, "alice", "private");

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                notificationService.getNotificationById(otherUserToken, created.getId()));
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    void GivenUnknownId_WhenGetById_ThenThrow() {
        assertThrows(NotFoundException.class, () ->
                notificationService.getNotificationById(userToken, "NOTIF-DOES-NOT-EXIST"));
    }

    // ── mark as read ────────────────────────────────────────────────────────

    @Test
    void GivenUnreadNotification_WhenMarkAsRead_ThenReturnTrueAndIsReadBecomesTrue() {
        NotificationDTO created = notificationService.createNotification(adminToken, "alice", "check this");
        assertFalse(created.isRead());

        boolean result = notificationService.markAsRead(userToken, created.getId());

        assertTrue(result);
        NotificationDTO fetched = notificationService.getNotificationById(userToken, created.getId());
        assertTrue(fetched.isRead());
    }

    @Test
    void GivenAlreadyRead_WhenMarkAsReadAgain_ThenReturnFalse() {
        NotificationDTO created = notificationService.createNotification(adminToken, "alice", "check this");
        notificationService.markAsRead(userToken, created.getId());

        boolean result = notificationService.markAsRead(userToken, created.getId());

        assertFalse(result);
    }

    @Test
    void GivenWrongUser_WhenMarkAsRead_ThenThrowAndStayUnread() {
        NotificationDTO created = notificationService.createNotification(adminToken, "alice", "private");

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                notificationService.markAsRead(otherUserToken, created.getId()));
        assertTrue(ex.getMessage().contains("Access denied"));

        NotificationDTO fetched = notificationService.getNotificationById(userToken, created.getId());
        assertFalse(fetched.isRead());
    }

    // ── unread count ────────────────────────────────────────────────────────

    @Test
    void GivenMultipleNotifications_WhenGetUnreadCount_ThenOnlyCountUnread() {
        String aliceId = "alice";
        NotificationDTO n1 = notificationService.createNotification(adminToken, aliceId, "msg-1");
        notificationService.createNotification(adminToken, aliceId, "msg-2");
        notificationService.createNotification(adminToken, aliceId, "msg-3");

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
        assertThrows(UnauthorizedException.class, () ->
                notificationService.getUnreadCount("bad-token"));
    }

    // ─── Concurrency Tests ───────────────────────────────────────────────────

    @Test
    void GivenMultipleAdmins_WhenConcurrentCreateNotification_ThenAllNotificationsHaveUniqueIds() throws Exception {
        int threadCount = 3;
        String[] targets = {"itay", "eden", "tomer"};
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        CopyOnWriteArrayList<String> createdIds = new CopyOnWriteArrayList<>();
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final String target = targets[i];
            executor.submit(() -> {
                try {
                    startLatch.await();
                    NotificationDTO dto = notificationService.createNotification(
                            adminToken, target, "Concurrent message for " + target);
                    createdIds.add(dto.getId());
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Concurrent creates must complete without deadlock");
        executor.shutdown();

        assertEquals(0, errorCount.get(), "No errors must occur during concurrent notification creation");
        assertEquals(threadCount, createdIds.size(), "Each thread must have created a notification");
        assertEquals(threadCount, createdIds.stream().distinct().count(),
                "All created notification IDs must be unique");
    }

    @Test
    void GivenAdminAndNonAdmin_WhenConcurrentCreateNotification_ThenOnlyAdminSucceeds() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Thread 1: valid admin token — must succeed
        new Thread(() -> {
            try {
                startLatch.await();
                notificationService.createNotification(adminToken, "tomer", "Admin message to tomer");
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        }).start();

        // Thread 2: non-admin user token — must be rejected with ForbiddenException
        new Thread(() -> {
            try {
                startLatch.await();
                notificationService.createNotification(userToken, "tomer", "Non-admin message to tomer");
                successCount.incrementAndGet();
            } catch (ForbiddenException e) {
                failureCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        }).start();

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Concurrent access must complete without deadlock");

        assertEquals(1, successCount.get(), "Only the admin thread must succeed");
        assertEquals(1, failureCount.get(), "The non-admin thread must be rejected");
    }

    @Test
    void GivenUnreadNotification_WhenConcurrentMarkAsRead_ThenExactlyOneThreadMarksItFirst() throws Exception {
        NotificationDTO created = notificationService.createNotification(adminToken, "alice", "itay sent this");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger trueCount = new AtomicInteger(0);
        AtomicInteger falseCount = new AtomicInteger(0);

        // Two threads both try to mark the same notification as read simultaneously
        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    boolean result = notificationService.markAsRead(userToken, created.getId());
                    if (result) trueCount.incrementAndGet();
                    else falseCount.incrementAndGet();
                } catch (Exception e) {
                    falseCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Concurrent markAsRead must complete without deadlock");

        // Correct thread-safe behavior: exactly one thread marks it as read first (returns true),
        // the second finds it already read (returns false).
        assertEquals(1, trueCount.get(),
                "Exactly one thread must be the first to mark the notification as read");
        assertEquals(1, falseCount.get(),
                "The second thread must find the notification already read");
    }

    @Test
    void GivenNotificationsForMultipleUsers_WhenConcurrentGetNotifications_ThenEachUserGetsOnlyTheirOwn()
            throws Exception {
        // Pre-create notifications: 2 for alice (itay), 1 for bob (eden)
        notificationService.createNotification(adminToken, "alice", "itay msg 1");
        notificationService.createNotification(adminToken, "alice", "itay msg 2");
        notificationService.createNotification(adminToken, "bob",   "eden msg 1");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        CopyOnWriteArrayList<Integer> aliceSizes = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<Integer> bobSizes   = new CopyOnWriteArrayList<>();

        // Thread 1: alice (itay) reads her notifications
        new Thread(() -> {
            try {
                startLatch.await();
                List<NotificationDTO> result = notificationService.getNotificationsForUser(userToken);
                aliceSizes.add(result.size());
            } catch (Exception e) {
                aliceSizes.add(-1);
            } finally {
                doneLatch.countDown();
            }
        }).start();

        // Thread 2: bob (eden) reads his notifications
        new Thread(() -> {
            try {
                startLatch.await();
                List<NotificationDTO> result = notificationService.getNotificationsForUser(otherUserToken);
                bobSizes.add(result.size());
            } catch (Exception e) {
                bobSizes.add(-1);
            } finally {
                doneLatch.countDown();
            }
        }).start();

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Concurrent reads must complete without deadlock");

        assertEquals(2, (int) aliceSizes.get(0), "alice (itay) must see exactly her 2 notifications");
        assertEquals(1, (int) bobSizes.get(0),   "bob (eden) must see exactly his 1 notification");
    }

    @Test
    void GivenSameUser_WhenConcurrentCreateSameMessage_ThenBothNotificationsStoredWithUniqueIds()
            throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        CopyOnWriteArrayList<String> ids = new CopyOnWriteArrayList<>();
        AtomicInteger errorCount = new AtomicInteger(0);

        // Two admin threads send the identical message to tomer at the same time
        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    NotificationDTO dto = notificationService.createNotification(
                            adminToken, "tomer", "Duplicate message to tomer");
                    ids.add(dto.getId());
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Concurrent identical creates must complete without deadlock");

        assertEquals(0, errorCount.get(), "Both create calls must succeed");
        assertEquals(2, ids.size(), "Both notifications must be stored");
        assertEquals(2, ids.stream().distinct().count(),
                "Both notifications must have different IDs — AtomicLong counter guarantees uniqueness");
    }
}
