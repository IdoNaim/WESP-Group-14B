package com.ticketpurchasingsystem.project.domain.User;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllUsersEvent;

@Component
public class UserListener implements ApplicationListener<GetAllUsersEvent> {

    private final IUserRepo userRepo;

    public UserListener(IUserRepo userRepo) {
        this.userRepo = userRepo;
    }

    //need to add Auth check to make sure only system admin can call this method
    //in the event there is a request id.
    @Override
    public void onApplicationEvent(GetAllUsersEvent event) {
        event.setResult(userRepo.findAll());
    }
}
