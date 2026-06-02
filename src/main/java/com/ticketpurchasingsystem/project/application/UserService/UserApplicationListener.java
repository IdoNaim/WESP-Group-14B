package com.ticketpurchasingsystem.project.application.UserService;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.AppointManagerEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.AssignOwnerEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.NewProdEvent;

import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.IsUserRegisteredEvent;
import com.ticketpurchasingsystem.project.domain.User.UserProduction;
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
}
