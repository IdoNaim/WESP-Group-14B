package com.ticketpurchasingsystem.project.domain.authTest;

import java.sql.Date;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

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

    // ── Concurrency tests ─────────────────────────────────────────────────────

    private DomainAuthService buildRealDomainAuthService() {
        InMemorySessionRepo realRepo = new InMemorySessionRepo();
        DomainAuthService svc = new DomainAuthService(realRepo);
        ReflectionTestUtils.setField(svc, "secret", SECRET);
        svc.init();
        return svc;
    }

    @Test
    void GivenMultipleUsers_WhenConcurrentCreateSession_ThenAllTokensStoredInRepo() throws Exception {
        // Arrange
        DomainAuthService svc = buildRealDomainAuthService();
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        CopyOnWriteArrayList<String> tokens = new CopyOnWriteArrayList<>();
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    tokens.add(svc.authenticateAndCreateSession("user-" + idx));
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Act
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Assert
        assertEquals(0, errorCount.get(), "No exceptions should occur during concurrent session creation");
        assertEquals(threadCount, tokens.size(), "Every thread must produce a token");
        for (String token : tokens) {
            assertTrue(svc.isSessionValid(token), "Every token must be valid immediately after creation");
        }
    }

    @Test
    void GivenSameUser_WhenConcurrentCreateSession_ThenNoExceptionsOccur() throws Exception {
        // Arrange
        DomainAuthService svc = buildRealDomainAuthService();
        int threadCount = 15;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    svc.authenticateAndCreateSession("shared-user");
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Act
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Assert
        assertEquals(0, errorCount.get(), "Concurrent logins for the same user must not throw exceptions");
    }

    @Test
    void GivenConcurrentInvalidations_WhenSameTokenDeletedByMultipleThreads_ThenNoExceptionAndTokenInvalid() throws Exception {
        // Arrange
        DomainAuthService svc = buildRealDomainAuthService();
        String token = svc.authenticateAndCreateSession(USERNAME);
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    svc.invalidateSession(token);
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Act
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Assert
        assertEquals(0, errorCount.get(), "Concurrent invalidations of the same token must not throw exceptions");
        assertFalse(svc.isSessionValid(token), "Token must be invalid after concurrent invalidations");
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

    // ── isAdmin tests ──────────────────────────────────────────────────────────

    @Nested
    class IsAdminTests {
    
        @Mock private ISessionRepo nestedSessionRepo;
    
        private DomainAuthService domainAuthService;
    
        private static String TOKEN;
    
        @BeforeEach
        void setUpDomainAuthService() {
            domainAuthService = new DomainAuthService(nestedSessionRepo);
        
            // Use a secure key for testing (32 bytes)
            String secureTestKey = "this-is-a-secure-test-key-32bytes!";
            ReflectionTestUtils.setField(domainAuthService, "secret", secureTestKey);
            domainAuthService.init();
        
            // Initialize TOKEN with valid claims
            Claims claims = Jwts.claims();
            claims.put("role", "admin");
            useRealJwtParsing(claims);
        }
    
        @Test
        void GivenValidAdminToken_WhenIsAdminSessionValid_ThenReturnTrue() {
            // Create claims with the "admin" role
            Claims claims = Jwts.claims();
            claims.put("role", "admin");

            // Generate a valid token
            SecretKey secureKey = Keys.hmacShaKeyFor("this-is-a-secure-test-key-32bytes!".getBytes());
            String validToken = Jwts.builder()
                    .setClaims(claims)
                    .setExpiration(new Date(System.currentTimeMillis() + 10000)) // Set expiration in the future
                    .signWith(secureKey)
                    .compact();

            // Mock the session repository to return the valid token
            when(nestedSessionRepo.findByToken(validToken))
                    .thenReturn(Optional.of(new SessionToken(validToken, System.currentTimeMillis() + 10000)));

            // Assert that the token is valid and the user is an admin
            assertTrue(domainAuthService.validateAdminSession(validToken));
        }
    
        @Test
        void GivenInvalidToken_WhenIsAdminSessionValid_ThenReturnFalse() {
            // Mock an invalid token
            when(nestedSessionRepo.findByToken(TOKEN)).thenReturn(Optional.empty());
    
            // Assert that the token is invalid
            assertFalse(domainAuthService.validateAdminSession(TOKEN));
        }
    
        @Test
        void GivenNonAdminRoleToken_WhenIsAdminSessionValid_ThenReturnFalse() {
            // Create claims with a non-admin role
            Claims claims = Jwts.claims();
            claims.put("role", "user");
    
            // Use real JWT parsing
            useRealJwtParsing(claims);
    
            // Assert that the token is valid but the user is not an admin
            assertFalse(domainAuthService.validateAdminSession(TOKEN));
        }
    
        @Test
        void GivenExpiredToken_WhenIsAdminSessionValid_ThenReturnFalse() {
            // Create claims with the "admin" role
            Claims claims = Jwts.claims();
            claims.put("role", "admin");

            // Generate a real expired token
            SecretKey secureKey = Keys.hmacShaKeyFor("this-is-a-secure-test-key-32bytes!".getBytes());
            String expiredToken = Jwts.builder()
                    .setClaims(claims)
                    .setExpiration(new Date(System.currentTimeMillis() - 1000)) // Set expiration in the past
                    .signWith(secureKey)
                    .compact();

            // Assert that the token is expired
            assertFalse(domainAuthService.validateAdminSession(expiredToken));
        }
    
        private void useRealJwtParsing(Claims claims) {
            // Generate a secure key for testing
            SecretKey secureKey = Keys.hmacShaKeyFor("this-is-a-secure-test-key-32bytes!".getBytes());
        
            // Generate a real JWT token with the provided claims
            String token = Jwts.builder()
                    .setClaims(claims) // Set the claims directly
                    .setExpiration(new Date(System.currentTimeMillis() + 1000)) // Set expiration time
                    .signWith(secureKey) // Use the secure signing key
                    .compact();
        
            // Update the TOKEN variable with the generated token
            TOKEN = token;
        }
    }
}