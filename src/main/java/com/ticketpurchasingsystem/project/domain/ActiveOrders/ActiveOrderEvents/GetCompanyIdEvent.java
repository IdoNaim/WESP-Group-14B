package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import org.springframework.context.ApplicationEvent;

public class GetCompanyIdEvent extends ApplicationEvent {
    String eventId;
    Integer result;
    public GetCompanyIdEvent(Object source, String eventId){
        super(source);
        this.eventId = eventId;
        this.result = null;
    }
    public void setResult(int compId){
        this.result = compId;
    }
    public int getResult(){
        return result;
    }
}
