package com.ticketpurchasingsystem.project.domain.authTest;

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

    @InjectMocks
    private DomainAuthService domainAuthService;

    private static final String SECRET = "my-super-secret-key-for-testing!";
    private static final String USERNAME = "alice";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(domainAuthService, "secret", SECRET);
        domainAuthService.init();
    }

    @Test
    void GivenUsername_WhenAuthenticateAndCreateSession_ThenTokenIsRealJwt() {
        // Arrange
        String inputUsername = USERNAME;

        // Act
        String token = domainAuthService.authenticateAndCreateSession(inputUsername);

        // Assert
        assertNotNull(token, "Token must not be null");
        assertEquals(3, token.split("\\.").length, "JWT must have 3 parts (header.payload.signature)");
        verify(sessionRepo, times(1)).save(any(SessionToken.class));
    }

    @Test
    void GivenUsername_WhenAuthenticateAndCreateSession_ThenTokenSubjectIsUsername() {
        // Arrange
        String inputUsername = USERNAME;
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());

        // Act
        String token = domainAuthService.authenticateAndCreateSession(inputUsername);
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // Assert
        assertEquals(inputUsername, claims.getSubject(), "Token subject must match the username");
    }

    @Test
    void GivenUsername_WhenAuthenticateAndCreateSession_ThenTokenExpiresInFuture() {
        // Arrange
        String inputUsername = USERNAME;
        long beforeCreation = System.currentTimeMillis();
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());

        // Act
        String token = domainAuthService.authenticateAndCreateSession(inputUsername);
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
        String inputUsername = USERNAME;
        ArgumentCaptor<SessionToken> captor = ArgumentCaptor.forClass(SessionToken.class);

        // Act
        String returnedToken = domainAuthService.authenticateAndCreateSession(inputUsername);

        // Assert
        verify(sessionRepo).save(captor.capture());
        assertEquals(returnedToken, captor.getValue().getToken(),
                "The returned token and the saved token must be identical");
    }

    @Test
    void GivenValidTokenInRepo_WhenIsSessionValid_ThenReturnTrue() {
        // Arrange
        String token = domainAuthService.authenticateAndCreateSession(USERNAME);
        long farFuture = System.currentTimeMillis() + 99999;
        when(sessionRepo.findByToken(token))
                .thenReturn(Optional.of(new SessionToken(token, farFuture)));

        // Act
        boolean valid = domainAuthService.isSessionValid(token);

        // Assert
        assertTrue(valid, "A freshly created token that exists in the repo must be valid");
    }

    @Test
    void GivenTokenRemovedFromRepo_WhenIsSessionValid_ThenReturnFalse() {
        // Arrange
        String token = domainAuthService.authenticateAndCreateSession(USERNAME);
        when(sessionRepo.findByToken(token)).thenReturn(Optional.empty());

        // Act
        boolean valid = domainAuthService.isSessionValid(token);

        // Assert
        assertFalse(valid, "A JWT that was removed from the repo must be invalid");
    }

    @Test
    void GivenRealToken_WhenGetUsernameFromToken_ThenReturnCorrectUsername() {
        // Arrange
        String inputUsername = USERNAME;
        String token = domainAuthService.authenticateAndCreateSession(inputUsername);

        // Act
        String extractedUsername = domainAuthService.getUsernameFromToken(token);

        // Assert
        assertEquals(inputUsername, extractedUsername, "Username extracted from the token must match the original");
    }

    @Test
    void GivenUsername_WhenFullLoginAndLogoutFlow_ThenSessionIsCreatedThenDestroyed() {
        // Arrange
        String inputUsername = USERNAME;
        String token = domainAuthService.authenticateAndCreateSession(inputUsername);
        long farFuture = System.currentTimeMillis() + 99999;

        when(sessionRepo.findByToken(token))
                .thenReturn(Optional.of(new SessionToken(token, farFuture)));

        // Act
        boolean validBeforeLogout = domainAuthService.isSessionValid(token);
        domainAuthService.invalidateSession(token);

        when(sessionRepo.findByToken(token)).thenReturn(Optional.empty());
        boolean validAfterLogout = domainAuthService.isSessionValid(token);

        // Assert
        assertTrue(validBeforeLogout, "Session must be valid right after login");
        assertFalse(validAfterLogout, "Session must be invalid after logout");
        verify(sessionRepo).deleteByToken(token);
    }
}