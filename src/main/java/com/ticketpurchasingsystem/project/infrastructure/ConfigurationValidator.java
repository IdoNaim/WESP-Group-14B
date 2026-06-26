package com.ticketpurchasingsystem.project.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationValidator implements InitializingBean {

    @Value("${spring.security.user.name}")
    private String securityUsername;

    @Value("${spring.security.user.password}")
    private String securityPassword;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.profiles.active}")
    private String activeProfiles;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (securityUsername == null || securityUsername.trim().isEmpty()) {
            throw new IllegalStateException("Required property 'spring.security.user.name' is missing or empty.");
        }
        if (securityPassword == null || securityPassword.trim().isEmpty()) {
            throw new IllegalStateException("Required property 'spring.security.user.password' is missing or empty.");
        }
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("Required property 'jwt.secret' is missing or empty.");
        }
        if (activeProfiles == null || activeProfiles.trim().isEmpty()) {
            throw new IllegalStateException("Required property 'spring.profiles.active' is missing or empty.");
        }
    }
}
