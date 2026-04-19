package com.ticketpurchasingsystem.project.domain.authentication;

import org.springframework.context.ApplicationEventPublisher;

public class AuthPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public AuthPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishNewSession(String userId, String sessionToken) {
        NewSessionEvent event = new NewSessionEvent(userId, sessionToken);
        eventPublisher.publishEvent(event);
    }
}