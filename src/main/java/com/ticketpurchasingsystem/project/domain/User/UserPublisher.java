package com.ticketpurchasingsystem.project.domain.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.domain.User.Events.GuestEvents.GuestEnterPlatformEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserRegistrationEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserLeavedPlatformEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserLogInEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserLogOutEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserNotFoundEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.GuestEvents.GuestLeavedPlatformEvent;
@Component
public class UserPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public UserPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishGuestEntered(String guestId, String sessionTokenStr) {
        eventPublisher.publishEvent(new GuestEnterPlatformEvent(guestId, sessionTokenStr));
    }

    public void publishUserCreated(String userId) {
        eventPublisher.publishEvent(new UserRegistrationEvent(userId));
    }

    public void publishUserLoggedOut(String userId, String sessionTokenStr) {
        eventPublisher.publishEvent(new UserLogOutEvent(userId, sessionTokenStr));
    }

    public void publishUserLoggedIn(String userId, String sessionTokenStr) {
        eventPublisher.publishEvent(new UserLogInEvent(userId, sessionTokenStr));
    }

    public void publishGuestExited(String guestId, String sessionTokenStr) {
        // You can create a GuestExitPlatformEvent similar to GuestEnterPlatformEvent and publish it here
        eventPublisher.publishEvent(new GuestLeavedPlatformEvent(guestId, sessionTokenStr));
    }

    public void publishUserLeftPlatform(String userId, String sessionTokenStr) {
        // You can create a UserLeftPlatformEvent similar to UserLogOutEvent and publish it here
        eventPublisher.publishEvent(new UserLeavedPlatformEvent(userId, sessionTokenStr));
    }

    public void publishUserNotFound(String userId) {
        eventPublisher.publishEvent(new UserNotFoundEvent(userId));
    }
}
