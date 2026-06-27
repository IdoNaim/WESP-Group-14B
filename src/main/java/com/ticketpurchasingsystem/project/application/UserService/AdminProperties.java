package com.ticketpurchasingsystem.project.application.UserService;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "admin")
public record AdminProperties(String id, String name, String email, String password) {}
