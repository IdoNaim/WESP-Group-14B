package com.ticketpurchasingsystem.project.domain.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.domain.User.Events.GuestEvents.GuestEnterPlatformEvent;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

@Component
public class UserPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public UserPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishGuestEntered(String guestId) {
        eventPublisher.publishEvent(new GuestEnterPlatformEvent(guestId));
    }

    public void publishUserCreated(String userId) {
        eventPublisher.publishEvent(new UserCreatedEvent(userId));
    }
}
