package com.ticketpurchasingsystem.project.acceptance.authentication;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class LoginAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";

    private AuthenticationService authService;

    @BeforeEach
    void setUp() {
        InMemorySessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        ReflectionTestUtils.setField(domainAuthService, "secret", TEST_SECRET);
        domainAuthService.init();
        authService = new AuthenticationService(domainAuthService, sessionRepo);
    }

    @Test
    void GivenValidUsername_WhenLogin_ThenNonNullTokenIsReturned() {
        String token = authService.login("eden");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void GivenValidUsername_WhenLogin_ThenTokenIsImmediatelyValid() {
        String token = authService.login("eden");

        assertTrue(authService.validate(token));
    }

    @Test
    void GivenValidUsername_WhenLogin_ThenUsernameIsRecoverableFromToken() {
        String token = authService.login("itay");

        assertEquals("itay", authService.getUser(token));
    }

    @Test
    void GivenTwoDifferentUsers_WhenBothLogin_ThenEachGetsDistinctToken() {
        String tokenA = authService.login("eden");
        String tokenB = authService.login("tomer");

        assertNotEquals(tokenA, tokenB);
        assertTrue(authService.validate(tokenA));
        assertTrue(authService.validate(tokenB));
    }

    @Test
    void GivenSameUser_WhenLoginTwice_ThenBothSessionsAreValid() {
        String first = authService.login("eden");
        String second = authService.login("eden");

        assertTrue(authService.validate(first));
        assertTrue(authService.validate(second));
    }

    // Fail scenarios

    @Test
    void GivenNeverIssuedToken_WhenValidate_ThenReturnFalse() {
        boolean result = authService.validate("not-a-valid-jwt-at-all");

        assertFalse(result);
    }

    @Test
    void GivenTamperedToken_WhenValidate_ThenReturnFalse() {
        String token = authService.login("eden");
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        boolean result = authService.validate(tampered);

        assertFalse(result);
    }
}
