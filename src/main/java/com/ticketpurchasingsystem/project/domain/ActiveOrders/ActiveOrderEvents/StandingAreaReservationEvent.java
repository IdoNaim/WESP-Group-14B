package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import org.springframework.context.ApplicationEvent;

public class StandingAreaReservationEvent extends ApplicationEvent {
    private final String eventId;
    private final String areaId;
    private final int quantity;
    private final String sessionToken;

    private Boolean result;

    public StandingAreaReservationEvent(Object source, String sessionToken, String eventId, String areaId, int quantity) {
        super(source);
        this.eventId = eventId;
        this.areaId = areaId;
        this.quantity = quantity;
        this.sessionToken = sessionToken;
        this.result = null;
    }

    public String getEventId() {
        return eventId;
    }

    public String getAreaId() {
        return areaId;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public int getQuantity() {
        return quantity;
    }

    public Boolean getResult() {
        return result;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }


    
}