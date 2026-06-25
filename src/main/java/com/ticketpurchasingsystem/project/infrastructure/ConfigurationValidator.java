package com.ticketpurchasingsystem.project.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class ConfigurationValidator {

    @Value("${spring.security.user.password:}")
    private String securityPassword;

    @PostConstruct
    public void validate() {
        if (securityPassword == null || securityPassword.trim().isEmpty()) {
            throw new IllegalStateException("Required property 'spring.security.user.password' is missing or empty.");
        }
    }
}
