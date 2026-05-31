package com.ticketpurchasingsystem.project.acceptance.user;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.UserService.UserPublisher;
import com.ticketpurchasingsystem.project.application.UserService.UserService;
import com.ticketpurchasingsystem.project.domain.User.Events.GuestEvents.GuestEnterPlatformEvent;
import com.ticketpurchasingsystem.project.domain.User.UserDTO;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.User.UserHandler;
import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import com.ticketpurchasingsystem.project.infrastructure.MemoryUserRepo;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class UserAcceptanceTests {

    private static final String JWT_SECRET = "myUltraSecretKeyForJWTSigningThatIsAtLeast32CharactersLong";
    private static final String USER_ID    = "alice";
    private static final String USER_NAME  = "Alice";
    private static final String USER_EMAIL = "alice@test.com";
    private static final String USER_PASS  = "password123";

    private static final String BOB_ID    = "bob";
    private static final String BOB_NAME  = "Bob";
    private static final String BOB_EMAIL = "bob@test.com";
    private static final String BOB_PASS  = "password456";

    private MemoryUserRepo userRepo;
    private UserService userService;
    private AuthenticationService authService;
    private String lastGuestToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepo = new MemoryUserRepo();
        InMemorySessionRepo sessionRepo = new InMemorySessionRepo();

        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        Field secretField = DomainAuthService.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(domainAuthService, JWT_SECRET);
        domainAuthService.init();

        authService = new AuthenticationService(domainAuthService, sessionRepo);
        UserHandler userHandler = new UserHandler();

        // captures any guest token published (covers both direct guestEntry and
        // the auto-guest created inside logoutUser)
        UserPublisher publisher = new UserPublisher(event -> {
            if (event instanceof GuestEnterPlatformEvent e) {
                lastGuestToken = e.getSessionToken();
            }
        });

        userService = new UserService(userRepo, userHandler, authService, publisher);
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
        assertDoesNotThrow(() -> userService.editPassword(USER_ID, USER_PASS, "newPass", sessionToken));
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
    }

    @Test
    void GivenLoggedInMember_WhenExit_ThenMemberIsStoredAsLoggedOut() {
        String sessionToken = registerAndLogin();
        
        userService.Exit(sessionToken);
        assertFalse(userRepo.findByID(USER_ID).isLoggedIn());
        assertNull(userRepo.findByID(USER_ID).getSessionTokenStr());
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
        userService.logoutUser(USER_ID, sessionToken); // lastGuestToken updated to the auto-created guest

        String newSession = userService.loginUser(USER_ID, "newPass456", lastGuestToken);
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
                userService.registerUser("tomer", "Tomer", "pass1", "tomer@test.com",
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
                userService.registerUser("tomer", "Tomer2", "pass2", "tomer2@test.com",
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
        userService.registerUser("itay", "Itay", "pass", "itay@test.com",
                UserGroupDiscount.NONE, guestToken);
        String sessionToken = userService.loginUser("itay", "pass", guestToken);

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
                userService.registerUser("eden", "Eden", "pass", "eden@test.com",
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
                userService.registerUser("tomer", "Tomer", "pass", "tomer@test.com",
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
