package com.ticketpurchasingsystem.project.domain.authentication;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.function.Function;

@Service
public class DomainAuthService {

    private final ISessionRepo sessionRepo;

    @Value("${jwt.secret}")
    private String secret;
    private SecretKey key;
    private final long expirationTime = 1000 * 60 * 60 * 2;

    public DomainAuthService(ISessionRepo sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String authenticateAndCreateSession(String username) {
        long expireTime = System.currentTimeMillis() + expirationTime;
        String tokenStr = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(expireTime))
                .signWith(key)
                .compact();

        sessionRepo.save(new SessionToken(tokenStr, expireTime));
        return tokenStr;
    }

    public boolean isSessionValid(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return sessionRepo.findByToken(token).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    public void invalidateSession(String token) {
        sessionRepo.deleteByToken(token);
    }
}