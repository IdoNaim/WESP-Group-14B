package com.ticketpurchasingsystem.project.acceptance.authentication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;

class LogoutAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";

    private AuthenticationService authService;

    @BeforeEach
    void setUp() {
        InMemorySessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        ReflectionTestUtils.setField(domainAuthService, "secret", TEST_SECRET);
        domainAuthService.init();
        authService = new AuthenticationService(domainAuthService, mock(SystemAdminService.class), sessionRepo);
    }

    @Test
    void GivenLoggedInUser_WhenLogout_ThenTokenIsInvalidated() {
        String token = authService.login("eden");

        authService.logout(token);

        assertFalse(authService.validate(token));
    }

    @Test
    void GivenLoggedInUser_WhenLogoutThenValidate_ThenFalseIsReturned() {
        String token = authService.login("tomer");
        assertTrue(authService.validate(token), "Token must be valid before logout");

        authService.logout(token);

        assertFalse(authService.validate(token), "Token must be invalid after logout");
    }

    @Test
    void GivenTwoDifferentUsers_WhenOneLogsOut_ThenOtherSessionIsUnaffected() {
        String tokenA = authService.login("eden");
        String tokenB = authService.login("tomer");

        authService.logout(tokenA);

        assertFalse(authService.validate(tokenA));
        assertTrue(authService.validate(tokenB));
    }

    // Fail

    @Test
    void GivenAlreadyLoggedOut_WhenLogoutAgain_ThenNoExceptionIsThrown() {
        String token = authService.login("eden");
        authService.logout(token);

        assertDoesNotThrow(() -> authService.logout(token));
        assertFalse(authService.validate(token));
    }

    @Test
    void GivenLoggedOut_WhenValidate_ThenReturnFalse() {
        String token = authService.login("eden");
        authService.logout(token);

        boolean result = authService.validate(token);

        assertFalse(result);
    }

    @Test
    void GivenUserNeverLoggedIn_WhenValidateArbitraryToken_ThenReturnFalse() {
        boolean result = authService.validate("some.random.token");

        assertFalse(result);
    }
}
