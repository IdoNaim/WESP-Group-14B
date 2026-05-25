package com.ticketpurchasingsystem.project.application;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;

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

    // ── Concurrency tests ─────────────────────────────────────────────────────

    private static final String TEST_SECRET = "my-super-secret-key-for-testing!";

    private AuthenticationService buildRealAuthService() {
        InMemorySessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService real = new DomainAuthService(sessionRepo);
        ReflectionTestUtils.setField(real, "secret", TEST_SECRET);
        real.init();
        return new AuthenticationService(real, mock(SystemAdminService.class), sessionRepo);
    }

    @Test
    void GivenMultipleUsers_WhenConcurrentLogin_ThenAllTokensAreValid() throws Exception {
        // Arrange
        AuthenticationService svc = buildRealAuthService();
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
                    tokens.add(svc.login("user-" + idx));
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
        assertEquals(0, errorCount.get(), "No exceptions should occur during concurrent logins");
        assertEquals(threadCount, tokens.size(), "Every thread must receive a token");
        for (String token : tokens) {
            assertTrue(svc.validate(token), "Every issued token must be valid");
        }
    }

    @Test
    void GivenConcurrentLoginAndLogout_WhenOneUserLogsOut_ThenOtherUsersUnaffected() throws Exception {
        // Arrange
        AuthenticationService svc = buildRealAuthService();
        String fixedToken = svc.login("fixed-user");
        int threadCount = 15;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        CopyOnWriteArrayList<String> newTokens = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    newTokens.add(svc.login("other-user-" + idx));
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Act
        startLatch.countDown();
        svc.logout(fixedToken);
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Assert
        assertFalse(svc.validate(fixedToken), "Fixed user token must be invalidated after logout");
        for (String token : newTokens) {
            assertTrue(svc.validate(token), "Other users' tokens must not be affected by the logout");
        }
    }
}