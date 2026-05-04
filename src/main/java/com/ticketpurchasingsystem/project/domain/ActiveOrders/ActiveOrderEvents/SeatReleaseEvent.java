package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import org.springframework.context.ApplicationEvent;

public class SeatReleaseEvent extends ApplicationEvent {
    private String eventID;
    private String[] seatsIds;
    public SeatReleaseEvent(Object source, String eventID, String[] seatIds){
        super(source);
        this.eventID= eventID;
        this.seatsIds = seatIds;
    }

    public String getEventID() {
        return eventID;
    }

    public String[] getSeatsIds() {
        return seatsIds;
    }
}
