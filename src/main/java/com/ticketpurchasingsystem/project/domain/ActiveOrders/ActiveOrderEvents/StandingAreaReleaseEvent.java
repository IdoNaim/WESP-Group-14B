package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import org.springframework.context.ApplicationEvent;

public class StandingAreaReleaseEvent extends ApplicationEvent {
    private String eventID;
    private String areaID;
    private int quantity;
    public StandingAreaReleaseEvent(Object source, String eventID, String areaID, int quantity){
        super(source);
        this.eventID = eventID;
        this.areaID = areaID;
        this.quantity = quantity;
    }

    public String getEventID() {
        return eventID;
    }

    public String getAreaID() {
        return areaID;
    }

    public int getQuantity() {
        return quantity;
    }
}
