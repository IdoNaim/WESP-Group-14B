package com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents;

import java.util.List;

import org.springframework.context.ApplicationEvent;

import com.ticketpurchasingsystem.project.domain.User.UserInfo;

public class GetAllUsersEvent extends ApplicationEvent {
    private List<UserInfo> result;
    

    public GetAllUsersEvent(Object source) {
        super(source);
    }

    public List<UserInfo> getResult(String reqId) { return result; }
    public void setResult(List<UserInfo> result) { this.result = result; }
}
