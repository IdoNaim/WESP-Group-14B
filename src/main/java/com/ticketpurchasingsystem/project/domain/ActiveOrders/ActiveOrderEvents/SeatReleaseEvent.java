package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import org.springframework.context.ApplicationEvent;

import java.util.List;

public class SeatReleaseEvent extends ApplicationEvent {
    private String eventID;
    private List<String> seatsIds;
    public SeatReleaseEvent(Object source, String eventID, List<String> seatIds){
        super(source);
        this.eventID= eventID;
        this.seatsIds = seatIds;
    }

    public String getEventID() {
        return eventID;
    }

    public List<String> getSeatsIds() {
        return seatsIds;
    }
}
