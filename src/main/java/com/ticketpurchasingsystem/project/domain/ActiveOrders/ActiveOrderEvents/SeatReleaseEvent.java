package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import org.springframework.context.ApplicationEvent;

import java.util.List;

public class SeatReleaseEvent extends ApplicationEvent {
    private String eventID;
    private List<String> seatsIds;
    private String orderID;
    private String sessionToken;
    public SeatReleaseEvent(Object source,String sessionToken, String eventID, List<String> seatIds, String orderId){
        super(source);
        this.eventID= eventID;
        this.seatsIds = seatIds;
        this.orderID = orderId;
        this.sessionToken = sessionToken;
    }

    public String getEventID() {
        return eventID;
    }

    public List<String> getSeatIds() {
        return seatsIds;
    }
    public String getOrderID() {
        return orderID;
    }

    public String getSessionToken() {
        return sessionToken;
    }
}
