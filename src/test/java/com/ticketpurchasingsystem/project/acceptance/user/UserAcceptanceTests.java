package com.ticketpurchasingsystem.project.acceptance.user;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        String loginGuestToken = enterAsGuest();
        return userService.loginUser(USER_ID, USER_PASS, loginGuestToken);
    }

    // ─── guestEntry ──────────────────────────────────────────────────────────

    @Test
    void GivenSystemIsRunning_WhenGuestEntry_ThenAGuestUserExistsInSystem() {
        enterAsGuest();

        boolean guestExists = userRepo.getAllUsers().stream().anyMatch(UserInfo::isGuest);
        assertTrue(guestExists);
    }

    // ─── registerUser ────────────────────────────────────────────────────────

    @Test
    void GivenGuestSession_WhenRegisterUser_ThenUserCanBeRetrieved() {
        String token = enterAsGuest();

        userService.registerUser(USER_ID, USER_NAME, USER_PASS, USER_EMAIL, UserGroupDiscount.NONE, token);

        UserDTO user = userService.getUser(USER_ID);
        assertEquals(USER_NAME, user.getUsername());
        assertEquals(USER_EMAIL, user.getEmail());
    }

    @Test
    void GivenExistingUserId_WhenRegisterUser_ThenThrowsException() {
        String token = enterAsGuest();
        userService.registerUser(USER_ID, USER_NAME, USER_PASS, USER_EMAIL, UserGroupDiscount.NONE, token);

        String anotherToken = enterAsGuest();
        assertThrows(RuntimeException.class, () ->
                userService.registerUser(USER_ID, "Other", USER_PASS, USER_EMAIL, UserGroupDiscount.NONE, anotherToken));
    }

    @Test
    void GivenInvalidSession_WhenRegisterUser_ThenThrowsException() {
        assertThrows(RuntimeException.class, () ->
                userService.registerUser(USER_ID, USER_NAME, USER_PASS, USER_EMAIL, UserGroupDiscount.NONE, "bad-token"));
    }

    // ─── loginUser ───────────────────────────────────────────────────────────

    @Test
    void GivenRegisteredUser_WhenLogin_ThenReturnsValidSessionToken() {
        String guestToken = enterAsGuest();
        userService.registerUser(USER_ID, USER_NAME, USER_PASS, USER_EMAIL, UserGroupDiscount.NONE, guestToken);

        String loginToken = enterAsGuest();
        String sessionToken = userService.loginUser(USER_ID, USER_PASS, loginToken);

        assertNotNull(sessionToken);
        assertFalse(sessionToken.isEmpty());
        assertTrue(authService.validate(sessionToken));
    }

    @Test
    void GivenWrongPassword_WhenLogin_ThenThrowsException() {
        String guestToken = enterAsGuest();
        userService.registerUser(USER_ID, USER_NAME, USER_PASS, USER_EMAIL, UserGroupDiscount.NONE, guestToken);

        String loginToken = enterAsGuest();
        assertThrows(RuntimeException.class, () ->
                userService.loginUser(USER_ID, "wrong-password", loginToken));
    }

    // ─── logoutUser ──────────────────────────────────────────────────────────

    @Test
    void GivenLoggedInUser_WhenLogout_ThenUserIsNoLongerLoggedIn() {
        String sessionToken = registerAndLogin();

        userService.logoutUser(USER_ID, sessionToken);

        assertFalse(userRepo.findByID(USER_ID).isLoggedIn());
    }

    // ─── Exit ────────────────────────────────────────────────────────────────

    @Test
    void GivenGuestSession_WhenExit_ThenGuestIsRemovedFromSystem() {
        String guestToken = enterAsGuest();
        String guestId = userRepo.getAllUsers().stream()
                .filter(UserInfo::isGuest).findFirst().orElseThrow().getId();

        userService.Exit(guestToken);

        assertNull(userRepo.findByID(guestId));
    }

    @Test
    void GivenLoggedInMember_WhenExit_ThenMemberIsStoredAsLoggedOut() {
        String sessionToken = registerAndLogin();

        userService.Exit(sessionToken);

        assertFalse(userRepo.findByID(USER_ID).isLoggedIn());
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
}
