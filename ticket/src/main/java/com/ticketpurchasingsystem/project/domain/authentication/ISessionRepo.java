package com.ticketpurchasingsystem.project.domain.authentication;


public interface ISessionRepo {
    
    void saveSession(SessionToken session);

    SessionToken getSessionByToken(String token);

    void deleteSession(String token);
}