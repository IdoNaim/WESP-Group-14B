package com.ticketpurchasingsystem.project.application;

import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;

import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final DomainAuthService domainAuthService;
    private final ISessionRepo sessionRepo;

    public AuthenticationService(DomainAuthService domainAuthService, ISessionRepo sessionRepo) {
        this.domainAuthService = domainAuthService;
        this.sessionRepo = sessionRepo;
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

    public void removeSessionManually(String token) {
        sessionRepo.deleteByToken(token);
    }
}