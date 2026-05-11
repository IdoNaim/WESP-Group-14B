package com.ticketpurchasingsystem.project.domain.ProductionTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.UserService.UserApplicationListener;
import com.ticketpurchasingsystem.project.application.UserService.UserPublisher;
import com.ticketpurchasingsystem.project.application.UserService.UserService;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.IsUserRegisteredEvent;
import com.ticketpurchasingsystem.project.domain.User.IUserRepo;
import com.ticketpurchasingsystem.project.domain.User.UserHandler;
import com.ticketpurchasingsystem.project.domain.User.UserInfo;

@ExtendWith(MockitoExtension.class)
class UserListenerIsRegisteredTests {

    @Mock
    private IUserRepo userRepo;
    @Mock
    private UserHandler userHandler;
    @Mock
    private UserInfo existingUser;
    @Mock
    private UserPublisher userPublisher;
    @Mock
    private DomainAuthService domainAuthService;
    @Mock
    private ISessionRepo sessionRepo;
    private UserApplicationListener listener;

    private static final String EXISTING_USER_ID = "user-1";
    private static final String UNKNOWN_USER_ID = "ghost-99";

    @BeforeEach
    void setUp() {
        listener = new UserApplicationListener(new UserService(userRepo, userHandler, new AuthenticationService(domainAuthService, sessionRepo),userPublisher ));
    }

    @Test
    void WhenOnIsUserRegisteredGivenExistingUser_ThenEventRegisteredIsTrue() {
        // Arrange
        when(userRepo.findByID(EXISTING_USER_ID)).thenReturn(existingUser);
        IsUserRegisteredEvent event = new IsUserRegisteredEvent(EXISTING_USER_ID);

        // Act
        listener.onIsUserRegistered(event);

        // Assert
        assertTrue(event.isRegistered());
    }

    @Test
    void WhenOnIsUserRegisteredGivenUnknownUser_ThenEventRegisteredIsFalse() {
        // Arrange
        when(userRepo.findByID(UNKNOWN_USER_ID)).thenReturn(null);
        IsUserRegisteredEvent event = new IsUserRegisteredEvent(UNKNOWN_USER_ID);

        // Act
        listener.onIsUserRegistered(event);

        // Assert
        assertFalse(event.isRegistered());
    }

    @Test
    void WhenOnIsUserRegisteredGivenUserId_ThenRepoCalledOnceWithCorrectId() {
        // Arrange
        when(userRepo.findByID(EXISTING_USER_ID)).thenReturn(existingUser);
        IsUserRegisteredEvent event = new IsUserRegisteredEvent(EXISTING_USER_ID);

        // Act
        listener.onIsUserRegistered(event);

        // Assert
        verify(userRepo, times(1)).findByID(EXISTING_USER_ID);
    }

    @Test
    void WhenOnIsUserRegisteredGivenUserId_ThenEventPreservesUserId() {
        // Arrange
        when(userRepo.findByID(EXISTING_USER_ID)).thenReturn(existingUser);
        IsUserRegisteredEvent event = new IsUserRegisteredEvent(EXISTING_USER_ID);

        // Act
        listener.onIsUserRegistered(event);

        // Assert
        assertEquals(EXISTING_USER_ID, event.getUserId());
    }
}
