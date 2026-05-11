package com.ticketpurchasingsystem.project.application.UserService;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.AssignOwnerEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.NewProdEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.GuestEvents.GuestEvents;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserEvents;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserLogInEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserLogOutEvent;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserRegistrationEvent;
import com.ticketpurchasingsystem.project.domain.User.UserProduction;

@Component
public class UserApplicationListener {

    private final UserService userService;

    public UserApplicationListener(UserService userService) {
        this.userService = userService;
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

    @EventListener
    public void onAddProductionRole(AssignOwnerEvent event) {
        userService.assignProductionRole(event.getAppointeeId(), event.getCompany().getCompanyId(), UserProduction.RoleInProduction.OWNER);
    }

    @EventListener
    public void onNewProduction(NewProdEvent event) {
        userService.assignProductionRole(event.getCompany().getFounderId(), event.getCompany().getCompanyId(), UserProduction.RoleInProduction.FOUNDER);
    }
}
