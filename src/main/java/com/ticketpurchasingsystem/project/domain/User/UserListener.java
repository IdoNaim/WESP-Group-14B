package com.ticketpurchasingsystem.project.domain.User;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.IsUserRegisteredEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.GuestEvents.GuestEvents;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserEvents;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserLogInEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserLogOutEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserRegistrationEvent;

@Component
public class UserListener {

    IUserRepo userRepo;
    UserHandler userHandler;

    public UserListener(IUserRepo userRepo, UserHandler userHandler) {
        this.userRepo = userRepo;
        this.userHandler = userHandler;
    }

    @EventListener
    public void onUserRegistered(UserRegistrationEvent event) {
        System.out.println("User created: " + event.getUserId());
    }

    @EventListener
    public void onUserLoggedIn(UserLogInEvent event) {
        System.out.println("User logged in: " + event.getUserId());
    }

    @EventListener
    public void onUserLoggedOut(UserLogOutEvent event) {
        System.out.println("User logged out: " + event.getUserId());
    }

    @EventListener
    public void onUserUpdated(UserEvents event) {
        System.out.println("User updated: " + event.getUserId());
    }

    @EventListener
    public void onUserDeleted(UserEvents event) {
        System.out.println("User deleted: " + event.getUserId());
    }

    @EventListener
    public void onExitPlatform(GuestEvents event) {
        System.out.println("User exited platform: " + event.getSessionToken());
    }

    // Cross-aggregate: Production asks whether a user is registered
    @EventListener
    public void onIsUserRegistered(IsUserRegisteredEvent event) {
        UserInfo user = userRepo.findByID(event.getUserId());
        event.setRegistered(user != null);
    }
}
