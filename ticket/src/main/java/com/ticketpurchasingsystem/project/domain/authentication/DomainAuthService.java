package com.ticketpurchasingsystem.project.domain.authentication;
import org.springframework.stereotype.Service;
@Service

class DomainAuthService {
    private final ISessionRepo sessionRepo;
    public DomainAuthService(ISessionRepo sessionRepo) {
        this.sessionRepo = sessionRepo;
    }
    
}