package com.ticketpurchasingsystem.project.domain.authentication;
import com.ticketpurchasingsystem.project.application.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthListener implements ApplicationListener<AuthenticationSuccessEvent> {

    @Autowired
    private AuthenticationService authenticationService;

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        String token = authenticationService.generateToken(username);
        System.out.println("New session created for user: " + username + " with token: " + token);
    }
    
}