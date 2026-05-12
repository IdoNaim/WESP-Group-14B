package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import org.springframework.context.ApplicationEvent;

public class StandingAreaReservationEvent extends ApplicationEvent {
    private final String eventId;
    private final String areaId;
    private final int quantity;

    private Boolean result;

    public StandingAreaReservationEvent(Object source, String eventId, String areaId, int quantity) {
        super(source);
        this.eventId = eventId;
        this.areaId = areaId;
        this.quantity = quantity;
        this.result = null;
    }

    public String getEventId() {
        return eventId;
    }

    public String getAreaId() {
        return areaId;
    }

    public int getAmount() {
        return quantity;
    }

    public Boolean getResult() {
        return result;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }
    
}