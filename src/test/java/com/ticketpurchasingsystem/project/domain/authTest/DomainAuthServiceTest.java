package com.ticketpurchasingsystem.project.domain.authTest;

import com.ticketpurchasingsystem.project.domain.authentication.AuthPublisher;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DomainAuthServiceTest {

    @Mock
    private ISessionRepo sessionRepo;

    @Mock
    private AuthPublisher authPublisher;

    @InjectMocks
    private DomainAuthService domainAuthService;

    private static final String SECRET = "my-super-secret-key-for-testing!";
    private static final String USERNAME = "alice";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(domainAuthService, "secret", SECRET);
        domainAuthService.init();
    }

    // Token creation

    @Test
    void GivenUsername_WhenAuthenticateAndCreateSession_ThenTokenIsRealJwt() {
        // Arrange
        doNothing().when(sessionRepo).save(any(SessionToken.class));
        doNothing().when(authPublisher).publishNewSession(anyString());

        // Act
        String token = domainAuthService.authenticateAndCreateSession(USERNAME);

        // Assert
        assertNotNull(token, "Token must not be null");
        assertEquals(3, token.split("\\.").length,
                "JWT must have 3 parts (header.payload.signature)");
    }

    @Test
    void GivenUsername_WhenAuthenticateAndCreateSession_ThenTokenSubjectIsUsername() {
        // Arrange
        doNothing().when(sessionRepo).save(any(SessionToken.class));
        doNothing().when(authPublisher).publishNewSession(anyString());

        // Act
        String token = domainAuthService.authenticateAndCreateSession(USERNAME);

        // Decode manually using the same secret to inspect claims
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // Assert
        assertEquals(USERNAME, claims.getSubject(),
                "Token subject must match the username it was created for");
    }

    @Test
    void GivenUsername_WhenAuthenticateAndCreateSession_ThenTokenExpiresInFuture() {
        // Arrange
        doNothing().when(sessionRepo).save(any(SessionToken.class));
        doNothing().when(authPublisher).publishNewSession(anyString());
        long beforeCreation = System.currentTimeMillis();

        // Act
        String token = domainAuthService.authenticateAndCreateSession(USERNAME);

        // Decode
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        long expiresAt = claims.getExpiration().getTime();
        long issuedAt = claims.getIssuedAt().getTime();
        long lifetimeMins = (expiresAt - issuedAt) / 1000 / 60;

        // Assert
        assertTrue(expiresAt > beforeCreation, "Token expiration must be in the future");
        assertTrue(lifetimeMins > 0, "Token must have a positive lifetime");
    }

    @Test
    void GivenUsername_WhenAuthenticateAndCreateSession_ThenSavedTokenMatchesReturned() {
        // Arrange
        ArgumentCaptor<SessionToken> captor = ArgumentCaptor.forClass(SessionToken.class);
        doNothing().when(authPublisher).publishNewSession(anyString());

        // Act
        String returnedToken = domainAuthService.authenticateAndCreateSession(USERNAME);

        // Assert
        verify(sessionRepo).save(captor.capture());
        assertEquals(returnedToken, captor.getValue().getToken(),
                "The returned token and the saved token must be identical");
    }

    // Session validation

    @Test
    void GivenValidTokenInRepo_WhenIsSessionValid_ThenReturnTrue() {
        // Arrange
        doNothing().when(sessionRepo).save(any());
        doNothing().when(authPublisher).publishNewSession(anyString());
        String token = domainAuthService.authenticateAndCreateSession(USERNAME);
        when(sessionRepo.findByToken(token))
                .thenReturn(Optional.of(new SessionToken(token, System.currentTimeMillis() + 99999)));

        // Act
        boolean valid = domainAuthService.isSessionValid(token);

        // Assert
        assertTrue(valid, "A freshly created token that exists in the repo must be valid");
    }

    @Test
    void GivenTokenRemovedFromRepo_WhenIsSessionValid_ThenReturnFalse() {
        // Arrange
        doNothing().when(sessionRepo).save(any());
        doNothing().when(authPublisher).publishNewSession(anyString());
        String token = domainAuthService.authenticateAndCreateSession(USERNAME);
        when(sessionRepo.findByToken(token)).thenReturn(Optional.empty());

        // Act
        boolean valid = domainAuthService.isSessionValid(token);

        // Assert
        assertFalse(valid,
                "A JWT that was removed from the repo must be invalid even if structurally correct");
    }

    // Username extraction

    @Test
    void GivenRealToken_WhenGetUsernameFromToken_ThenReturnCorrectUsername() {
        // Arrange
        doNothing().when(sessionRepo).save(any());
        doNothing().when(authPublisher).publishNewSession(anyString());
        String token = domainAuthService.authenticateAndCreateSession(USERNAME);

        // Act
        String extractedUsername = domainAuthService.getUsernameFromToken(token);

        // Assert
        assertEquals(USERNAME, extractedUsername,
                "Username extracted from the token must match the one it was created for");
    }

    // full logon and logout flow
    @Test
    void GivenUsername_WhenFullLoginAndLogoutFlow_ThenSessionIsCreatedThenDestroyed() {
        // Arrange
        doNothing().when(sessionRepo).save(any());
        doNothing().when(authPublisher).publishNewSession(anyString());

        // Act — login
        String token = domainAuthService.authenticateAndCreateSession(USERNAME);

        // Act — validate before logout
        when(sessionRepo.findByToken(token))
                .thenReturn(Optional.of(new SessionToken(token, System.currentTimeMillis() + 99999)));
        boolean validBeforeLogout = domainAuthService.isSessionValid(token);

        // Act — logout
        domainAuthService.invalidateSession(token);

        // Act — validate after logout
        when(sessionRepo.findByToken(token)).thenReturn(Optional.empty());
        boolean validAfterLogout = domainAuthService.isSessionValid(token);

        // Assert
        assertTrue(validBeforeLogout, "Session must be valid right after login");
        assertFalse(validAfterLogout, "Session must be invalid after logout");
        verify(sessionRepo).deleteByToken(token);
    }
}