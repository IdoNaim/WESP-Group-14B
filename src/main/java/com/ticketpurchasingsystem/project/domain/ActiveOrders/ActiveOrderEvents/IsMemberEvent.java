package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import org.springframework.context.ApplicationEvent;

public class IsMemberEvent extends ApplicationEvent {
    private String userId;
    private boolean isMember;
    public IsMemberEvent(Object source, String userId){
        super(source);
        this.userId = userId;
        this.isMember = false;
    }
    public boolean getResult(){
        return isMember;
    }
    public void setAnswer(boolean isMember){
        this.isMember = isMember;
    }
    public String getUserId() {
        return this.userId;
    }

}
