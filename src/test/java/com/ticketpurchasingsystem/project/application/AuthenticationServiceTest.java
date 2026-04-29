package com.ticketpurchasingsystem.project.application;

import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private DomainAuthService domainAuthService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private static final String USERNAME = "testUser";
    private static final String TOKEN = "mock.jwt.token";

    // ─── login ───────────────────────────────────────────────────────────────

    @Test
    void GivenValidUsername_WhenLogin_ThenReturnToken() {
        // Arrange
        when(domainAuthService.authenticateAndCreateSession(USERNAME)).thenReturn(TOKEN);

        // Act
        String result = authenticationService.login(USERNAME);

        // Assert
        assertEquals(TOKEN, result);
        verify(domainAuthService, times(1)).authenticateAndCreateSession(USERNAME);
    }

    @Test
    void GivenValidUsername_WhenLogin_ThenDelegatesAuthToService() {
        // Arrange
        when(domainAuthService.authenticateAndCreateSession(USERNAME)).thenReturn(TOKEN);

        // Act
        authenticationService.login(USERNAME);

        // Assert
        verify(domainAuthService).authenticateAndCreateSession(USERNAME);
    }

    // ─── validate ────────────────────────────────────────────────────────────

    @Test
    void GivenValidToken_WhenValidate_ThenReturnTrue() {
        // Arrange
        when(domainAuthService.isSessionValid(TOKEN)).thenReturn(true);

        // Act
        boolean result = authenticationService.validate(TOKEN);

        // Assert
        assertTrue(result);
        verify(domainAuthService).isSessionValid(TOKEN);
    }

    @Test
    void GivenExpiredToken_WhenValidate_ThenReturnFalse() {
        // Arrange
        when(domainAuthService.isSessionValid(TOKEN)).thenReturn(false);

        // Act
        boolean result = authenticationService.validate(TOKEN);

        // Assert
        assertFalse(result);
        verify(domainAuthService).isSessionValid(TOKEN);
    }

    // ─── logout ──────────────────────────────────────────────────────────────

    @Test
    void GivenValidToken_WhenLogout_ThenInvalidatesSession() {
        // Arrange
        doNothing().when(domainAuthService).invalidateSession(TOKEN);

        // Act
        authenticationService.logout(TOKEN);

        // Assert
        verify(domainAuthService, times(1)).invalidateSession(TOKEN);
    }

    // ─── getUser ─────────────────────────────────────────────────────────────

    @Test
    void GivenValidToken_WhenGetUser_ThenReturnUsername() {
        // Arrange
        when(domainAuthService.getUsernameFromToken(TOKEN)).thenReturn(USERNAME);

        // Act
        String result = authenticationService.getUser(TOKEN);

        // Assert
        assertEquals(USERNAME, result);
        verify(domainAuthService).getUsernameFromToken(TOKEN);
    }

    @Test
    void GivenInvalidToken_WhenGetUser_ThenThrowException() {
        // Arrange
        when(domainAuthService.getUsernameFromToken(TOKEN))
                .thenThrow(new RuntimeException("Invalid token"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> authenticationService.getUser(TOKEN));
        verify(domainAuthService).getUsernameFromToken(TOKEN);
    }
}