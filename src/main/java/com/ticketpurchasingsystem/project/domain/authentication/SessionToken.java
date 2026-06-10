package com.ticketpurchasingsystem.project.domain.authentication;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Sessions")
public class SessionToken {
    @Id
    @Column(name = "token")
    private String token;
    @Column(name =  "expirationTime")
    private long expirationTime;

    public SessionToken(String token, long expirationTime) {
        this.token = token;

        this.expirationTime = expirationTime;
    }

    public String getToken() {
        return token;
    }

    public long getExpirationTime() {
        return expirationTime;
    }
}