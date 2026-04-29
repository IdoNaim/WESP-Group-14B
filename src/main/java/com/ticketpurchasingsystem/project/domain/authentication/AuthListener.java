package com.ticketpurchasingsystem.project.domain.authentication;

import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AuthListener implements ApplicationListener<AuthenticationSuccessEvent> {

    private final DomainAuthService domainAuthService;

    public AuthListener(DomainAuthService domainAuthService) {
        this.domainAuthService = domainAuthService;
    }

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        domainAuthService.authenticateAndCreateSession(username);
    }

    @EventListener
    public void handleNewSessionEvent(NewSessionEvent event) {
        String token = event.getSessionToken();
        String user = domainAuthService.getUsernameFromToken(token);
        System.out.println("Audit: User " + user + " session published.");
    }

}