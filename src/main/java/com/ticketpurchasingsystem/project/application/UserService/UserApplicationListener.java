package com.ticketpurchasingsystem.project.application.UserService;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.AppointManagerEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.AssignOwnerEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.NewProdEvent;

import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.IsUserRegisteredEvent;
import com.ticketpurchasingsystem.project.domain.User.UserProduction;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserLeavedPlatformEvent;
import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllUsersEvent;

@Component
public class UserApplicationListener {

    private final UserService userService;

    public UserApplicationListener(UserService userService) {
        this.userService = userService;
    }

    @EventListener
    public void onAddProductionRole(AssignOwnerEvent event) {
        userService.assignProductionRole(event.getAppointeeId(), event.getCompany().getCompanyId(), UserProduction.RoleInProduction.OWNER);
    }

    @EventListener
    public void onNewProduction(NewProdEvent event) {
        userService.assignProductionRole(event.getCompany().getFounderId(), event.getCompany().getCompanyId(), UserProduction.RoleInProduction.FOUNDER);
    }
    // Cross-aggregate: Production asks whether a user is registered
    @EventListener
    public void onIsUserRegistered(IsUserRegisteredEvent event) {
        boolean isRegistered = userService.isUserRegistered(event.getUserId());
        event.setRegistered(isRegistered);
    }

    @EventListener
    public void onAppointManagerEvent(AppointManagerEvent event) {
        userService.assignProductionRole(event.getManagerId(), event.getCompany().getCompanyId(), UserProduction.RoleInProduction.MANAGER);
    }

    @EventListener
    public void onGetAllUsers(GetAllUsersEvent event) {
        event.setResult(userService.getAllUsers());
    }

    // Irregular exit: the presence WebSocket (infrastructure) detected a dropped
    // connection and published this event. React here in the application layer so
    // the WebSocket detail never leaks into the domain.
    @EventListener
    public void onUserLeftPlatform(UserLeavedPlatformEvent event) {
        userService.handleDisconnect(event.getUserId(), event.getSessionToken());
    }
}
