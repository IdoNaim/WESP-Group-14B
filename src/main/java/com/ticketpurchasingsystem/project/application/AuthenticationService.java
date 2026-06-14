package com.ticketpurchasingsystem.project.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AuthenticationService {

    private final DomainAuthService domainAuthService;
    private final ISessionRepo sessionRepo;

    @Autowired
    public AuthenticationService(DomainAuthService domainAuthService, ISessionRepo sessionRepo) {
        this.domainAuthService = domainAuthService;
        this.sessionRepo = sessionRepo;
    }
    @Transactional
    public String login(String username) {
        return domainAuthService.authenticateAndCreateSession(username);
    }
    public String login(String username,String role) {
        
        return domainAuthService.authenticateAndCreateSessionAdmin(username);
    }

    @Transactional(readOnly = true)
    public boolean validate(String token) {
        return domainAuthService.isSessionValid(token);
    }

    @Transactional
    public void logout(String token) {
        domainAuthService.invalidateSession(token);
    }
    @Transactional(readOnly = true)
    public String getUser(String token) {
        return domainAuthService.getUsernameFromToken(token);
    }
    @Transactional
    public void removeSessionManually(String token) {
        sessionRepo.deleteByToken(token);
    }
    @Transactional(readOnly = true)
    public boolean isAdmin(String token) {
        try{
            return domainAuthService.validateAdminSession(token);
        } catch (RuntimeException e) {
            return false;
        }
    }
}