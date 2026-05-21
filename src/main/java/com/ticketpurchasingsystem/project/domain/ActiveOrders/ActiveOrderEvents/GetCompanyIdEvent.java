package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import org.springframework.context.ApplicationEvent;

public class GetCompanyIdEvent extends ApplicationEvent {
    private String eventId;
    private Integer result;
    public GetCompanyIdEvent(Object source, String eventId){
        super(source);
        this.eventId = eventId;
        this.result = null;
    }
    public void setResult(Integer compId){
        this.result = compId;
    }
    public Integer getResult(){
        return result;
    }
    public String getEventId() {
        return eventId;
    }
}
