package com.ticketpurchasingsystem.project.domain.User;

import org.springframework.context.event.EventListener;
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

}