package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import org.springframework.context.ApplicationEvent;

public class IsValidEventIDEvent extends ApplicationEvent {
    private String eventId;
    private boolean isValid = false; // The listener will set this

    public IsValidEventIDEvent(Object source, String eventId) {
        super(source);
        this.eventId = eventId;
    }

    public String getEventId() { return eventId; }
    
    public boolean isValid() { return isValid; }
    
    public void setValid(boolean valid) { this.isValid = valid; }
}
