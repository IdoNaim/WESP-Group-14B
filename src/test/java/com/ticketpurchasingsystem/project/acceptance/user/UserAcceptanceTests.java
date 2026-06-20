package com.ticketpurchasingsystem.project.acceptance.user;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.UserService.UserService;
import com.ticketpurchasingsystem.project.domain.User.IUserRepo;
import com.ticketpurchasingsystem.project.domain.User.UserDTO;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@ActiveProfiles("test")
class UserAcceptanceTests {

    private static final String USER_ID    = "alice";
    private static final String USER_NAME  = "Alice";
    private static final String USER_EMAIL = "alice@test.com";
    private static final String USER_PASS  = "password123";

    private static final String BOB_ID    = "bob";
    private static final String BOB_NAME  = "Bob";
    private static final String BOB_EMAIL = "bob@test.com";
    private static final String BOB_PASS  = "password456";

    @Autowired
    private IUserRepo userRepo;

    // Autowired alongside IUserRepo so the whole flow runs against the real
    // Spring-managed, DB-backed session store (DBSessionRepo is @Primary) rather
    // than a hand-built in-memory stub. Because the autowired services below are
    // @Transactional proxies, the session repo's transaction-bound deleteByToken
    // now runs inside a transaction, so guest sessions are actually removed on
    // logout/exit instead of lingering in the DB.
    @Autowired
    private ISessionRepo sessionRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationService authService;

    @BeforeEach
    void setUp() {
        // Start from a clean users table so a previous test's leftovers (or the
        // admin-1 seed) do not skew the user-count assertions on the shared DB.
        userRepo.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepo.deleteAll();
    }

    /** Calls guestEntry and returns the token that was just issued. */
    private String enterAsGuest() {
        return userService.guestEntry();
    }

    /** Registers USER_ID and logs them in; returns the active session token. */
    private String registerAndLogin() {
        String guestToken = enterAsGuest();
        userService.registerUser(USER_ID, USER_NAME, USER_PASS, USER_EMAIL, UserGroupDiscount.NONE, guestToken);
        return userService.loginUser(USER_ID, USER_PASS, guestToken);
    }

    // ─── tokenValidation ──────────────────────────────────────────────────────
    @Test
    void GivenValidToken_WhenEveryMethodCalled_ThenNotThrowsException() {
        String token = enterAsGuest();
        assertDoesNotThrow(() -> userService.registerUser(USER_ID, USER_NAME, USER_PASS, USER_EMAIL, UserGroupDiscount.NONE, token));
        String sessionToken = userService.loginUser(USER_ID, USER_PASS, token);
        assertDoesNotThrow(() -> userService.editEmail(USER_ID, USER_EMAIL, "newemail@test.com", sessionToken));
        assertDoesNotThrow(() -> userService.editPassword(USER_ID, USER_PASS, "newPass1", sessionToken));
        assertDoesNotThrow(() -> userService.editUsername(USER_ID, USER_NAME, "NewName", sessionToken));
        assertDoesNotThrow(() -> userService.setUserGroupDiscount(USER_ID, UserGroupDiscount.STUDENT, sessionToken));
        String newGuestToken = userService.logoutUser(USER_ID, sessionToken);
        assertDoesNotThrow(() -> userService.Exit(newGuestToken));
    }

    @Test
    void GivenInvalidToken_WhenEveryMethodCalled_ThenReturnsFalse() {
        assertThrows(RuntimeException.class, () -> userService.registerUser(USER_ID, USER_NAME, USER_PASS, USER_EMAIL, UserGroupDiscount.NONE, "invalid-token"));
        assertThrows(RuntimeException.class, () -> userService.loginUser(USER_ID, USER_PASS, "invalid-token"));
        assertThrows(RuntimeException.class, () -> userService.editEmail(USER_ID, USER_EMAIL, "newemail@test.com", "invalid-token"));
        assertThrows(RuntimeException.class, () -> userService.editPassword(USER_ID, USER_PASS, "newPass", "invalid-token"));
        assertThrows(RuntimeException.class, () -> userService.editUsername(USER_ID, USER_NAME, "NewName", "invalid-token"));
        assertThrows(RuntimeException.class, () -> userService.setUserGroupDiscount(USER_ID, UserGroupDiscount.STUDENT, "invalid-token"));
        assertThrows(RuntimeException.class, () -> userService.logoutUser(USER_ID, "invalid-token"));
        assertThrows(RuntimeException.class, () -> userService.Exit("invalid-token"));
    }

    // ─── guestEntry ──────────────────────────────────────────────────────────

    @Test
    void GivenSystemIsRunning_WhenGuestEntry_ThenAGuestUserExistsInSystem() {
        String guestToken = enterAsGuest();

        String userId = authService.getUser(guestToken);
        UserInfo guestInfo = userRepo.findByID(userId);
        assertNotNull(guestInfo);
        assertTrue(guestInfo.isGuest());
        assertEquals(guestToken, guestInfo.getSessionTokenStr());
    }

    // ─── registerUser ────────────────────────────────────────────────────────

    @Test
    void GivenGuestSession_WhenRegisterUser_ThenUserCanBeRetrieved() {
        String token = enterAsGuest();

        userService.registerUser(USER_ID, USER_NAME, USER_PASS, USER_EMAIL, UserGroupDiscount.NONE, token);

        UserDTO user = userService.getUser(USER_ID);
        assertEquals(USER_ID, user.getUserId());
        assertEquals(USER_NAME, user.getUsername());
        assertEquals(USER_EMAIL, user.getEmail());
    }

    @Test
    void GivenExistingUserId_WhenRegisterUser_ThenThrowsException() {
        int initialUserCount = userRepo.getAllUsers().size();
        String token = enterAsGuest();
        initialUserCount += 1;
        userService.registerUser(USER_ID, USER_NAME, USER_PASS, USER_EMAIL, UserGroupDiscount.NONE, token);
        initialUserCount += 1;
        assertEquals(initialUserCount, userRepo.getAllUsers().size());

        String anotherToken = enterAsGuest();
        initialUserCount += 1;
        assertThrows(RuntimeException.class, () ->
                userService.registerUser(USER_ID, "Other", USER_PASS, USER_EMAIL, UserGroupDiscount.NONE, anotherToken));
        initialUserCount += 1;
        assertNotEquals(initialUserCount, userRepo.getAllUsers().size());
    }

    @Test
    void GivenInvalidSession_WhenRegisterUser_ThenThrowsException() {
        int initialUserCount = userRepo.getAllUsers().size();
        assertThrows(RuntimeException.class, () ->
                userService.registerUser(USER_ID, USER_NAME, USER_PASS, USER_EMAIL, UserGroupDiscount.NONE, "bad-token"));
        assertEquals(initialUserCount, userRepo.getAllUsers().size());
    }   

    // ─── loginUser ───────────────────────────────────────────────────────────

    @Test
    void GivenRegisteredUser_WhenLogin_ThenReturnsValidSessionToken() {
        int userCountBefore = userRepo.getAllUsers().size();
        String sessionToken = registerAndLogin(); 
        int userCountAfter = userRepo.getAllUsers().size();

        assertEquals(userCountBefore + 1 , userCountAfter);
        assertNotNull(sessionToken);
        assertFalse(sessionToken.isEmpty());
        assertTrue(authService.validate(sessionToken));
    }

    @Test
    void GivenWrongPassword_WhenLogin_ThenThrowsException() {
        String guestToken = enterAsGuest();
        userService.registerUser(USER_ID, USER_NAME, USER_PASS, USER_EMAIL, UserGroupDiscount.NONE, guestToken);

        int userCountBeforeLogin = userRepo.getAllUsers().size();
        assertThrows(RuntimeException.class, () ->
                userService.loginUser(USER_ID, "wrong-password", guestToken));
        int userCountAfterLogin = userRepo.getAllUsers().size();
        assertEquals(userCountBeforeLogin, userCountAfterLogin); // failed login should not change user count
    }

    @Test
    void GivenUserId_WhenLogin_ThenThrowsException() {
        String guestToken = enterAsGuest();
        userService.registerUser(USER_ID, USER_NAME, USER_PASS, USER_EMAIL, UserGroupDiscount.NONE, guestToken);

        int userCountBeforeLogin = userRepo.getAllUsers().size();
        assertThrows(RuntimeException.class, () ->
                userService.loginUser("non-existent-user", USER_PASS, guestToken));
        int userCountAfterLogin = userRepo.getAllUsers().size();
        assertEquals(userCountBeforeLogin, userCountAfterLogin); // failed login should not change user count
    }



    // ─── logoutUser ──────────────────────────────────────────────────────────

    @Test
    void GivenLoggedInUser_WhenLogout_ThenUserIsNoLongerLoggedIn() {
        String sessionToken = registerAndLogin();
        int userCountBeforeLogout = userRepo.getAllUsers().size();
        System.out.println("User count before logout: " + userCountBeforeLogout);
        String newGuestToken = userService.logoutUser(USER_ID, sessionToken);
        int userCountAfterLogout = userRepo.getAllUsers().size();

        assertFalse(userRepo.findByID(USER_ID).isLoggedIn());
        assertNotNull(newGuestToken);
        assertEquals(userCountBeforeLogout, userCountAfterLogout - 1); // for the new guest created during logout
    }

    @Test
    void GivenUnLoggedInUser_WhenLogout_ThenThrowsException() {
        String guestToken = enterAsGuest();
        userService.registerUser(USER_ID, USER_NAME, USER_PASS, USER_EMAIL, UserGroupDiscount.NONE, guestToken);
        int userCountBefore = userRepo.getAllUsers().size();
        assertThrows(RuntimeException.class, () ->
                userService.logoutUser(USER_ID, "invalid-token"));
        int userCountAfter = userRepo.getAllUsers().size();
        assertEquals(userCountBefore, userCountAfter); // failed logout should not change user count
    }


    // ─── Exit ────────────────────────────────────────────────────────────────

    @Test
    void GivenGuestSession_WhenExit_ThenGuestIsRemovedFromSystem() {
        String guestToken = enterAsGuest();
        String guestId = authService.getUser(guestToken);

        userService.Exit(guestToken);
        assertNull(userRepo.findByID(guestId));
        assertTrue(sessionRepo.findByToken(guestToken).isEmpty(),
                "Guest session must be deleted from the session store on exit, not left lingering in the DB");
    }

    @Test
    void GivenLoggedInMember_WhenExit_ThenMemberIsStoredAsLoggedOut() {
        String sessionToken = registerAndLogin();

        userService.Exit(sessionToken);
        assertFalse(userRepo.findByID(USER_ID).isLoggedIn());
        assertNull(userRepo.findByID(USER_ID).getSessionTokenStr());
    }

    // ─── Irregular exit (abandoned guest) ─────────────────────────────────────

    @Test
    void GivenGuestAbandonsWithoutExit_WhenSessionExpires_ThenCleanupRemovesGuestAndSession() {
        // An "irregular exit": a guest who never calls Exit and whose session has
        // already expired. We persist an expired session + guest directly because
        // the JWT's 2h clock cannot be fast-forwarded in a unit test.
        String expiredToken = "expired-guest-token";
        String abandonedGuestId = "abandoned-guest";
        sessionRepo.save(new SessionToken(expiredToken, System.currentTimeMillis() - 1_000));
        userRepo.store(new UserInfo(abandonedGuestId, expiredToken));

        // A second guest with a still-valid session that must survive the sweep.
        String liveToken = enterAsGuest();
        String liveGuestId = authService.getUser(liveToken);

        userService.purgeExpiredSessions();

        // The abandoned guest and its expired session are gone…
        assertNull(userRepo.findByID(abandonedGuestId));
        assertTrue(sessionRepo.findByToken(expiredToken).isEmpty(),
                "Expired guest session must be purged from the DB on cleanup");
        // …while the live guest and its session are left untouched.
        assertNotNull(userRepo.findByID(liveGuestId));
        assertTrue(sessionRepo.findByToken(liveToken).isPresent(),
                "A guest with a valid (unexpired) session must not be purged");
    }

    // ─── Irregular exit (logged-in member) ───────────────────────────────────

    @Test
    void GivenMemberExitedIrregularly_WhenReLoginWithCorrectPassword_ThenSucceedsAndStaleSessionRemoved() {
        // Member logs in, then closes the browser (X) — nothing resets logged_in.
        registerAndLogin();
        // Model the leftover from the irregular exit as a distinct stale session
        // (a real re-login seconds later yields a different JWT; we pin a known
        // value so the assertion doesn't depend on the token clock's resolution).
        String staleToken = "stale-session-token";
        UserInfo member = userRepo.findByID(USER_ID);
        member.setLoggedIn(true);
        member.setSessionTokenStr(staleToken);
        userRepo.store(member);
        sessionRepo.save(new SessionToken(staleToken, System.currentTimeMillis() + 3_600_000));

        // They reopen the app (fresh guest) and log in again — takeover.
        String guestToken = enterAsGuest();
        String secondSession = userService.loginUser(USER_ID, USER_PASS, guestToken);

        assertNotNull(secondSession);
        assertTrue(authService.validate(secondSession));

        UserInfo after = userRepo.findByID(USER_ID);
        assertTrue(after.isLoggedIn());
        assertEquals(secondSession, after.getSessionTokenStr());
        assertTrue(sessionRepo.findByToken(staleToken).isEmpty(),
                "the stale session from the irregular exit must be taken over (removed)");
    }

    @Test
    void GivenMemberExitedIrregularly_WhenReLoginWithWrongPassword_ThenThrowsAndStaleSessionKept() {
        String firstSession = registerAndLogin();
        String guestToken = enterAsGuest();

        assertThrows(RuntimeException.class, () ->
                userService.loginUser(USER_ID, "wrong-password", guestToken));

        // A failed re-login must not take over: the existing session stays intact.
        UserInfo member = userRepo.findByID(USER_ID);
        assertTrue(member.isLoggedIn());
        assertEquals(firstSession, member.getSessionTokenStr());
        assertTrue(sessionRepo.findByToken(firstSession).isPresent(),
                "a wrong-password re-login must not drop the existing session");
    }

    @Test
    void GivenLoggedInMember_WhenHandleDisconnect_ThenLoggedOutAndSessionRemoved() {
        // Simulates the WebSocket disconnect callback (browser closed without Exit).
        String session = registerAndLogin();

        userService.handleDisconnect(USER_ID, session);

        UserInfo member = userRepo.findByID(USER_ID);
        assertFalse(member.isLoggedIn(), "an irregular disconnect must log the member out");
        assertNull(member.getSessionTokenStr());
        assertTrue(sessionRepo.findByToken(session).isEmpty(),
                "the dropped connection's session must be removed");
    }

    @Test
    void GivenMemberReLoggedInUnderNewToken_WhenStaleDisconnectArrives_ThenIgnored() {
        // Race: the user re-logged-in (takeover) before the OLD socket's close event
        // was processed. A late disconnect for a token that is no longer current must
        // no-op, leaving the live session intact.
        String currentSession = registerAndLogin();
        String oldToken = "old-stale-token"; // a token the member no longer holds

        userService.handleDisconnect(USER_ID, oldToken);

        UserInfo member = userRepo.findByID(USER_ID);
        assertTrue(member.isLoggedIn(),
                "a disconnect for a non-current token must not log the member out");
        assertEquals(currentSession, member.getSessionTokenStr());
        assertTrue(sessionRepo.findByToken(currentSession).isPresent(),
                "the current session must survive a stale disconnect");
    }

    @Test
    void GivenMemberAbandonsWithoutExit_WhenSessionExpires_ThenSweepLogsOutMemberButKeepsLiveOne() {
        // Backstop for a missed disconnect (e.g. server restart): the sweep must
        // log out a member whose session expired, keeping the account.
        String expiredToken = "expired-member-token";
        UserInfo expiredMember = new UserInfo("carol", "Carol", "carol@test.com", "password123", UserGroupDiscount.NONE);
        expiredMember.setLoggedIn(true);
        expiredMember.setSessionTokenStr(expiredToken);
        userRepo.store(expiredMember);
        sessionRepo.save(new SessionToken(expiredToken, System.currentTimeMillis() - 1_000));

        // A live member who must stay logged in.
        String liveSession = registerAndLogin();

        userService.purgeExpiredSessions();

        UserInfo swept = userRepo.findByID("carol");
        assertNotNull(swept, "the member account must be kept — only the dead session goes");
        assertFalse(swept.isLoggedIn(), "an expired member must be logged out by the sweep");
        assertNull(swept.getSessionTokenStr());
        assertTrue(sessionRepo.findByToken(expiredToken).isEmpty(),
                "the expired member session must be purged");

        UserInfo live = userRepo.findByID(USER_ID);
        assertTrue(live.isLoggedIn(), "a member with a valid session must stay logged in");
        assertTrue(sessionRepo.findByToken(liveSession).isPresent());
    }

    // ─── getAllUsers ──────────────────────────────────────────────────────────

    @Test
    void GivenRegisteredUsers_WhenGetAllUsers_ThenTheirDTOsAreReturned() {
        String token = enterAsGuest();
        userService.registerUser(USER_ID, USER_NAME, USER_PASS, USER_EMAIL, UserGroupDiscount.NONE, token);

        var users = userService.getAllUsers();

        assertTrue(users.stream().anyMatch(u -> USER_ID.equals(u.getUserId())));
    }

    // ─── getUser ─────────────────────────────────────────────────────────────

    @Test
    void GivenExistingUser_WhenGetUser_ThenReturnCorrectDTO() {
        String token = enterAsGuest();
        userService.registerUser(USER_ID, USER_NAME, USER_PASS, USER_EMAIL, UserGroupDiscount.NONE, token);

        UserDTO result = userService.getUser(USER_ID);

        assertEquals(USER_NAME, result.getUsername());
        assertEquals(USER_EMAIL, result.getEmail());
    }

    @Test
    void GivenNonExistingUser_WhenGetUser_ThenThrowsException() {
        assertThrows(RuntimeException.class, () -> userService.getUser("ghost"));
    }

    // ─── deleteUser ──────────────────────────────────────────────────────────

    @Test
    void GivenLoggedInUser_WhenDeleteUser_ThenUserNoLongerExistsInSystem() {
        String sessionToken = registerAndLogin();

        userService.deleteUser(USER_ID, sessionToken);

        assertThrows(RuntimeException.class, () -> userService.getUser(USER_ID));
    }

    // ─── editUsername ────────────────────────────────────────────────────────

    @Test
    void GivenLoggedInUser_WhenEditUsername_ThenGetUserReturnsNewName() {
        String sessionToken = registerAndLogin();

        userService.editUsername(USER_ID, USER_NAME, "Alice New", sessionToken);

        assertEquals("Alice New", userService.getUser(USER_ID).getUsername());
    }

    // ─── editPassword ────────────────────────────────────────────────────────

    @Test
    void GivenLoggedInUser_WhenEditPassword_ThenUserCanLoginWithNewPassword() {
        String sessionToken = registerAndLogin();

        userService.editPassword(USER_ID, USER_PASS, "newPass456", sessionToken);
        // logoutUser returns the token of the auto-created guest, which we reuse to log back in
        String newGuestToken = userService.logoutUser(USER_ID, sessionToken);

        String newSession = userService.loginUser(USER_ID, "newPass456", newGuestToken);
        assertNotNull(newSession);
    }

    // ─── editEmail ───────────────────────────────────────────────────────────

    @Test
    void GivenLoggedInUser_WhenEditEmail_ThenGetUserReturnsNewEmail() {
        String sessionToken = registerAndLogin();

        userService.editEmail(USER_ID, USER_EMAIL, "new@test.com", sessionToken);

        assertEquals("new@test.com", userService.getUser(USER_ID).getEmail());
    }

    // ─── setUserGroupDiscount ────────────────────────────────────────────────

    @Test
    void GivenLoggedInUser_WhenSetUserGroupDiscount_ThenGetUserReturnsNewDiscount() {
        String sessionToken = registerAndLogin();

        userService.setUserGroupDiscount(USER_ID, UserGroupDiscount.STUDENT, sessionToken);

        assertEquals(UserGroupDiscount.STUDENT, userService.getUser(USER_ID).getGroupDiscount());
    }

    // ─── Unauthorized Edits ──────────────────────────────────────────────────

    @Test
    void GivenLoggedInUser_WhenEditAnotherUsersEmail_ThenThrowsException() {
        String aliceToken = registerAndLogin();
        String bobGuest = enterAsGuest();
        userService.registerUser(BOB_ID, BOB_NAME, BOB_PASS, BOB_EMAIL, UserGroupDiscount.NONE, bobGuest);

        assertThrows(RuntimeException.class, () ->
                userService.editEmail(BOB_ID, BOB_EMAIL, "hacked@test.com", aliceToken));
    }

    @Test
    void GivenLoggedInUser_WhenDeleteAnotherUser_ThenThrowsException() {
        String aliceToken = registerAndLogin();
        String bobGuest = enterAsGuest();
        userService.registerUser(BOB_ID, BOB_NAME, BOB_PASS, BOB_EMAIL, UserGroupDiscount.NONE, bobGuest);

        assertThrows(RuntimeException.class, () ->
                userService.deleteUser(BOB_ID, aliceToken));
    }

    // ─── Invalid/Expired Session Tokens ──────────────────────────────────────

    @Test
    void GivenInvalidToken_WhenEditPassword_ThenThrowsException() {
        assertThrows(RuntimeException.class, () ->
                userService.editPassword(USER_ID, USER_PASS, "newPass", "invalid-token"));
    }

    @Test
    void GivenLoggedOutUser_WhenAttemptToEditProfile_ThenThrowsException() {
        String sessionToken = registerAndLogin();
        userService.logoutUser(USER_ID, sessionToken);

        assertThrows(RuntimeException.class, () ->
                userService.editEmail(USER_ID, USER_EMAIL, "new@test.com", sessionToken));
    }

    // ─── Bad Input Validation ────────────────────────────────────────────────

    @Test
    void GivenValidSession_WhenRegisterWithInvalidEmailFormat_ThenThrowsException() {
        String guestToken = enterAsGuest();

        assertThrows(RuntimeException.class, () ->
                userService.registerUser(USER_ID, USER_NAME, USER_PASS, "not-an-email", UserGroupDiscount.NONE, guestToken));
    }

    @Test
    void GivenValidSession_WhenEditPasswordToEmptyString_ThenThrowsException() {
        String sessionToken = registerAndLogin();

        assertThrows(RuntimeException.class, () ->
                userService.editPassword(USER_ID, USER_PASS, "", sessionToken));
    }

    // ─── Concurrency Tests ───────────────────────────────────────────────────

    @Test
    void GivenSameUserId_WhenConcurrentRegister_ThenAtMostOneSucceedsAndNoDuplicateExists() throws Exception {
        String itayToken = enterAsGuest();
        String edenToken = enterAsGuest();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        new Thread(() -> {
            try {
                startLatch.await();
                userService.registerUser("tomer", "Tomer", "pass123", "tomer@test.com",
                        UserGroupDiscount.NONE, itayToken);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        }).start();

        new Thread(() -> {
            try {
                startLatch.await();
                userService.registerUser("tomer", "Tomer2", "pass456", "tomer2@test.com",
                        UserGroupDiscount.NONE, edenToken);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        }).start();

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Concurrent register must complete without deadlock");

        assertNotNull(userRepo.findByID("tomer"), "User tomer must exist after at least one registration");
        assertEquals(1, userRepo.getAllUsers().stream()
                        .filter(u -> "tomer".equals(u.getId())).count(),
                "Exactly one user entry must exist for the contested userId — no duplicates allowed");
    }

    @Test
    void GivenMultipleGuests_WhenConcurrentGuestEntry_ThenAllGetUniqueValidTokens() throws Exception {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        CopyOnWriteArrayList<String> tokens = new CopyOnWriteArrayList<>();
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    tokens.add(userService.guestEntry());
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Concurrent guest entry must complete without deadlock");
        executor.shutdown();

        assertEquals(0, errorCount.get(), "No errors should occur during concurrent guest entries");
        assertEquals(threadCount, tokens.size(), "Each thread must receive a token");
        assertEquals(threadCount, tokens.stream().distinct().count(), "All issued tokens must be unique");
        for (String token : tokens) {
            assertTrue(authService.validate(token), "Every issued token must be valid");
        }
    }

    @Test
    void GivenLoggedInUser_WhenConcurrentDelete_ThenUserIsRemovedWithoutDeadlock() throws Exception {
        String guestToken = enterAsGuest();
        userService.registerUser("itay", "Itay", "pass123", "itay@test.com",
                UserGroupDiscount.NONE, guestToken);
        String sessionToken = userService.loginUser("itay", "pass123", guestToken);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    userService.deleteUser("itay", sessionToken);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                    // expected for the second thread when user is already deleted
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Concurrent delete must complete without deadlock");
        assertNull(userRepo.findByID("itay"), "User itay must be deleted after concurrent delete");
    }

    @Test
    void GivenInvalidAndValidSession_WhenConcurrentRegister_ThenOnlyValidSessionSucceeds() throws Exception {
        String edenToken = enterAsGuest();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        new Thread(() -> {
            try {
                startLatch.await();
                userService.registerUser("eden", "Eden", "pass123", "eden@test.com",
                        UserGroupDiscount.NONE, edenToken);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        }).start();

        new Thread(() -> {
            try {
                startLatch.await();
                userService.registerUser("tomer", "Tomer", "pass123", "tomer@test.com",
                        UserGroupDiscount.NONE, "invalid-token");
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        }).start();

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Concurrent register must complete without deadlock");

        assertEquals(1, successCount.get(), "Only the thread with a valid session should succeed");
        assertEquals(1, failureCount.get(), "The thread with an invalid session must fail");
        assertNotNull(userRepo.findByID("eden"), "eden must be registered");
        assertNull(userRepo.findByID("tomer"), "tomer must not be registered due to invalid session");
    }
}
