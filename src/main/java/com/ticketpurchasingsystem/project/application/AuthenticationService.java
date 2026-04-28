package com.ticketpurchasingsystem.project.application;

import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final DomainAuthService domainAuthService;

    public AuthenticationService(DomainAuthService domainAuthService) {
        this.domainAuthService = domainAuthService;
    }

    public String login(String username) {
        return domainAuthService.authenticateAndCreateSession(username);
    }

    public boolean validate(String token) {
        return domainAuthService.isSessionValid(token);
    }

    public void logout(String token) {
        domainAuthService.invalidateSession(token);
    }

    public String getUser(String token) {
        return domainAuthService.getUsernameFromToken(token);
    }
}