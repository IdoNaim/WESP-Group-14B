package com.ticketpurchasingsystem.project.domain.authentication;

import java.util.List;
import java.util.Optional;

public interface ISessionRepo {
    void save(SessionToken sessionToken);

    Optional<SessionToken> findByToken(String token);

    void deleteByToken(String token);

    List<SessionToken> findAll();
}