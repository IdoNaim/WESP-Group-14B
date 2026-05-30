package com.ticketpurchasingsystem.project.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;

import com.ticketpurchasingsystem.project.application.UserService.UserPublisher;
import com.ticketpurchasingsystem.project.domain.User.Events.GuestEvents.GuestEnterPlatformEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.GuestEvents.GuestLeavedPlatformEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserLeavedPlatformEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserLogInEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserLogOutEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserNotFoundEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserRegistrationEvent;

class UserPublisherTests {

    private RecordingApplicationEventPublisher eventPublisher;
    private UserPublisher publisher;

    @BeforeEach
    void setUp() {
        eventPublisher = new RecordingApplicationEventPublisher();
        publisher = new UserPublisher(eventPublisher);
    }

    @Test
    void GivenGuestIdAndSessionToken_WhenPublishGuestEntered_ThenPublishGuestEnterPlatformEventCalled() {
        publisher.publishGuestEntered("guest-1", "token-1");

        assertPublishedEvent(GuestEnterPlatformEvent.class);
        GuestEnterPlatformEvent event = (GuestEnterPlatformEvent) eventPublisher.publishedEvents.get(0);
        assertEquals("guest-1", event.getGuestId());
        assertEquals("token-1", event.getSessionToken());
    }

    @Test
    void GivenPublisherFails_WhenPublishGuestEntered_ThenExceptionThrown() {
        eventPublisher.failOnPublish = true;

        assertThrows(RuntimeException.class, () -> publisher.publishGuestEntered("guest-1", "token-1"));
    }

    @Test
    void GivenUserId_WhenPublishUserCreated_ThenPublishUserRegistrationEventCalled() {
        publisher.publishUserCreated("user-1");

        assertPublishedEvent(UserRegistrationEvent.class);
        UserRegistrationEvent event = (UserRegistrationEvent) eventPublisher.publishedEvents.get(0);
        assertEquals("user-1", event.getUserId());
    }

    @Test
    void GivenPublisherFails_WhenPublishUserCreated_ThenExceptionThrown() {
        eventPublisher.failOnPublish = true;

        assertThrows(RuntimeException.class, () -> publisher.publishUserCreated("user-1"));
    }

    @Test
    void GivenUserIdAndToken_WhenPublishUserLoggedOut_ThenPublishUserLogOutEventCalled() {
        publisher.publishUserLoggedOut("user-1", "token-1");

        assertPublishedEvent(UserLogOutEvent.class);
        UserLogOutEvent event = (UserLogOutEvent) eventPublisher.publishedEvents.get(0);
        assertEquals("user-1", event.getUserId());
        assertEquals("token-1", event.getSessionToken());
    }

    @Test
    void GivenPublisherFails_WhenPublishUserLoggedOut_ThenExceptionThrown() {
        eventPublisher.failOnPublish = true;

        assertThrows(RuntimeException.class, () -> publisher.publishUserLoggedOut("user-1", "token-1"));
    }

    @Test
    void GivenUserIdAndToken_WhenPublishUserLoggedIn_ThenPublishUserLogInEventCalled() {
        publisher.publishUserLoggedIn("user-1", "token-1");

        assertPublishedEvent(UserLogInEvent.class);
        UserLogInEvent event = (UserLogInEvent) eventPublisher.publishedEvents.get(0);
        assertEquals("user-1", event.getUserId());
        assertEquals("token-1", event.getSessionToken());
    }

    @Test
    void GivenPublisherFails_WhenPublishUserLoggedIn_ThenExceptionThrown() {
        eventPublisher.failOnPublish = true;

        assertThrows(RuntimeException.class, () -> publisher.publishUserLoggedIn("user-1", "token-1"));
    }

    @Test
    void GivenGuestIdAndToken_WhenPublishGuestExited_ThenPublishGuestLeavedPlatformEventCalled() {
        publisher.publishGuestExited("guest-1", "token-1");

        assertPublishedEvent(GuestLeavedPlatformEvent.class);
        GuestLeavedPlatformEvent event = (GuestLeavedPlatformEvent) eventPublisher.publishedEvents.get(0);
        assertEquals("guest-1", event.getGuestId());
        assertEquals("token-1", event.getSessionToken());
    }

    @Test
    void GivenPublisherFails_WhenPublishGuestExited_ThenExceptionThrown() {
        eventPublisher.failOnPublish = true;

        assertThrows(RuntimeException.class, () -> publisher.publishGuestExited("guest-1", "token-1"));
    }

    @Test
    void GivenUserIdAndToken_WhenPublishUserLeftPlatform_ThenPublishUserLeavedPlatformEventCalled() {
        publisher.publishUserLeftPlatform("user-1", "token-1");

        assertPublishedEvent(UserLeavedPlatformEvent.class);
        UserLeavedPlatformEvent event = (UserLeavedPlatformEvent) eventPublisher.publishedEvents.get(0);
        assertEquals("user-1", event.getUserId());
        assertEquals("token-1", event.getSessionToken());
    }

    @Test
    void GivenPublisherFails_WhenPublishUserLeftPlatform_ThenExceptionThrown() {
        eventPublisher.failOnPublish = true;

        assertThrows(RuntimeException.class, () -> publisher.publishUserLeftPlatform("user-1", "token-1"));
    }

    @Test
    void GivenUserId_WhenPublishUserNotFound_ThenPublishUserNotFoundEventCalled() {
        publisher.publishUserNotFound("user-1");

        assertPublishedEvent(UserNotFoundEvent.class);
        UserNotFoundEvent event = (UserNotFoundEvent) eventPublisher.publishedEvents.get(0);
        assertEquals("user-1", event.getUserId());
    }

    @Test
    void GivenPublisherFails_WhenPublishUserNotFound_ThenExceptionThrown() {
        eventPublisher.failOnPublish = true;

        assertThrows(RuntimeException.class, () -> publisher.publishUserNotFound("user-1"));
    }

    private void assertPublishedEvent(Class<?> expectedClass) {
        assertEquals(1, eventPublisher.publishedEvents.size());
        assertEquals(expectedClass, eventPublisher.publishedEvents.get(0).getClass());
    }

    private static final class RecordingApplicationEventPublisher implements ApplicationEventPublisher {
        private final List<Object> publishedEvents = new ArrayList<>();
        private boolean failOnPublish;

        @Override
        public void publishEvent(@NonNull Object event) {
            if (failOnPublish) {
                throw new RuntimeException("publisher failed");
            }
            publishedEvents.add(event);
        }

        @Override
        public void publishEvent(@NonNull ApplicationEvent event) {
            publishEvent((Object) event);
        }
    }
}