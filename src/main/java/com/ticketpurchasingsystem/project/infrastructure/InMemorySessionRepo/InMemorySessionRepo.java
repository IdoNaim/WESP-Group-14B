package com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo;

import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import org.springframework.stereotype.Repository;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class InMemorySessionRepo implements ISessionRepo {
    private final ConcurrentHashMap<String, SessionToken> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(SessionToken sessionToken) {
        sessions.put(sessionToken.getToken(), sessionToken);
    }

    @Override
    public Optional<SessionToken> findByToken(String token) {
        return Optional.ofNullable(sessions.get(token));
    }

    @Override
    public void deleteByToken(String token) {
        sessions.remove(token);
    }

    @Override
    public List<SessionToken> findAll() {
        return new ArrayList<>(sessions.values());
    }
}