package com.ticketpurchasingsystem.project.acceptance.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import com.ticketpurchasingsystem.project.application.SystemAdminService;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;

class ValidateTokenAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";

    private InMemorySessionRepo sessionRepo;
    private AuthenticationService authService;

    @BeforeEach
    void setUp() {
        sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        ReflectionTestUtils.setField(domainAuthService, "secret", TEST_SECRET);
        domainAuthService.init();
        authService = new AuthenticationService(domainAuthService, sessionRepo);
    }

    @Test
    void GivenFreshToken_WhenValidate_ThenReturnTrue() {
        String token = authService.login("eden");

        assertTrue(authService.validate(token));
    }

    @Test
    void GivenValidToken_WhenGetUser_ThenReturnCorrectUsername() {
        String token = authService.login("eden");

        assertEquals("eden", authService.getUser(token));
    }

    @Test
    void GivenMultipleUsers_WhenValidateEachToken_ThenAllReturnTrue() {
        String[] users = { "eden", "tomer", "itay" };

        for (String user : users) {
            String token = authService.login(user);
            assertTrue(authService.validate(token), "Token for " + user + " must be valid");
        }
    }

    // Fail scenarios

    @Test
    void GivenNeverIssuedToken_WhenValidate_ThenReturnFalse() {
        boolean result = authService.validate("completely.invalid.token");

        assertFalse(result);
    }

    @Test
    void GivenTokenManuallyRemovedFromRepo_WhenValidate_ThenReturnFalse() {
        String token = authService.login("eden");
        authService.removeSessionManually(token);

        boolean result = authService.validate(token);

        assertFalse(result);
    }

    @Test
    void GivenLoggedOutToken_WhenValidate_ThenReturnFalse() {
        String token = authService.login("eden");
        authService.logout(token);

        boolean result = authService.validate(token);

        assertFalse(result);
    }

    @Test
    void GivenInvalidJwtString_WhenGetUser_ThenExceptionIsThrown() {
        assertThrows(Exception.class, () -> authService.getUser("not-a-real-jwt"));
    }
}
