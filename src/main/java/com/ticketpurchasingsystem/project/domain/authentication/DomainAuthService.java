package com.ticketpurchasingsystem.project.domain.authentication;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

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
                .claim("role", "User") // Add role claim for authorization
                .signWith(key)
                .compact();

        sessionRepo.save(new SessionToken(tokenStr, expireTime));
        return tokenStr;
    }

    public String authenticateAndCreateSessionAdmin(String username) {
        long expireTime = System.currentTimeMillis() + expirationTime;
        String tokenStr = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(expireTime))
                .claim("role", "admin") // Add role claim for authorization
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

    public boolean validateAdminSession(String token) {
        if (isSessionValid(token)) {
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                // Check if the role is "admin"
                String role = claims.get("role", String.class);
                return "admin".equals(role);
            } catch (ExpiredJwtException e) {
                // Token is expired
                return false;
            } catch (Exception e) {
                // Token is invalid for other reasons
                return false;
            }
        }
        return false;

    }
}