package com.ticketpurchasingsystem.project.domain.systemAdmin;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;

import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllUsersEvent;

class AdminPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public AdminPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public List<UserInfo> publishGetAllUsers(String reqId) {
        GetAllUsersEvent event = new GetAllUsersEvent(this);
        eventPublisher.publishEvent(event);
        return event.getResult(reqId);
    }
}
