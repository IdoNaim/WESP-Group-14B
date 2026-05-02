package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import org.springframework.context.ApplicationEvent;

public class SeatReservationEvent extends ApplicationEvent{
    String[] seatIds;
    String eventID;
    Boolean result;

    public SeatReservationEvent(Object source, String eventID ,String[] seatIds) {
        super(source);
        this.seatIds = seatIds;
        this.eventID = eventID;
        result = null;
    }

    public String[] getSeatIds() {
        return seatIds;
    }

    public String getEventID() {
        return eventID;
    }

    public void setResult(boolean result) {
        this.result = result;
    }
    
    //returns null if not set yet
    public Boolean getResult() {
        return result;
    }

    
}
