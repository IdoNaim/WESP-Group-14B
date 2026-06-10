package com.ticketpurchasingsystem.project.domain.authentication;

import java.util.List;
import java.util.Optional;

public interface ISessionRepo {
    SessionToken save(SessionToken sessionToken);

    Optional<SessionToken> findByToken(String token);

    void deleteByToken(String token);

    List<SessionToken> findAll();
}