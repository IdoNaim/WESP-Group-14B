package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import org.springframework.context.ApplicationEvent;

import java.util.List;

public class SeatReleaseEvent extends ApplicationEvent {
    private String eventID;
    private List<String> seatsIds;
    private String orderID;
    public SeatReleaseEvent(Object source, String eventID, List<String> seatIds, String orderId){
        super(source);
        this.eventID= eventID;
        this.seatsIds = seatIds;
        this.orderID = orderId;
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
}
