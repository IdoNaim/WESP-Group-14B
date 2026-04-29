package com.ticketpurchasingsystem.project.domain.User;

<<<<<<< HEAD
<<<<<<< HEAD
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.domain.User.Events.GuestEvents.GuestEvents;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserEvents;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserLogInEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserLogOutEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserRegistrationEvent;
import com.ticketpurchasingsystem.project.domain.authentication.NewSessionEvent;
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
            // Handle the event, e.g., update the read model or notify other services
            System.out.println("User created: " + event.getUserId());
        }

        @EventListener
        public void onUserLoggedIn(UserLogInEvent event) {
            // Handle the event, e.g., update the read model or notify other services
            System.out.println("User logged in: " + event.getUserId());
        }

        @EventListener
        public void onUserLoggedOut(UserLogOutEvent event) {
            // Handle the event, e.g., update the read model or notify other services
            System.out.println("User logged out: " + event.getUserId());
        }

        @EventListener
        public void onUserUpdated(UserEvents event) {
            // Handle the event, e.g., update the read model or notify other services
            System.out.println("User updated: " + event.getUserId());
        }

        @EventListener
        public void onUserDeleted(UserEvents event) {
            // Handle the event, e.g., update the read model or notify other services
            System.out.println("User deleted: " + event.getUserId());
        }

        @EventListener
        public void onEnterPlatform(NewSessionEvent event) {
            // Handle the event, e.g., update the read model or notify other services
            System.out.println("guest " + event.getUserId() + "entered platform with token: " + event.getSessionToken());
            userHandler.handleGuestEntry(userRepo, event.getSessionToken());
        }

        @EventListener
        public void onExitPlatform(GuestEvents event) {
            // Handle the event, e.g., update the read model or notify other services
            System.out.println("User exited platform: " + event.getSessionToken());
        }


    
=======
import org.springframework.context.ApplicationListener;
=======
import org.springframework.context.event.EventListener;
>>>>>>> origin/SESSION_TOKEN
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllEvent;


@Component
public class UserListener {

    private final IUserRepo userRepo;

    public UserListener(IUserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @EventListener
    public void handleGetAllUsers(GetAllEvent<UserInfo> event) {
        event.setResult(userRepo.findAll());
    }
<<<<<<< HEAD
>>>>>>> 33_generate_Id
}
=======

}
>>>>>>> origin/SESSION_TOKEN
