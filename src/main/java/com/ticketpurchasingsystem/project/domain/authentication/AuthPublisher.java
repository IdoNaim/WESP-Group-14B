package com.ticketpurchasingsystem.project.domain.authentication;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class AuthPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public AuthPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishNewSession(String sessionToken) {
        NewSessionEvent event = new NewSessionEvent(sessionToken);
        eventPublisher.publishEvent(event);
    }
}