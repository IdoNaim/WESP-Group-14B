package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import java.util.List;

import org.springframework.context.ApplicationEvent;

public class CheckSeatsReservedEvent extends ApplicationEvent {

    private String sessionToken;
    private String eventId;
    private List<String> seatIds;
    private List<String> result;
    private String orderId;

    public CheckSeatsReservedEvent(Object source, String sessionToken, String orderId, String eventId, List<String> seatIds) {
        super(source);
        this.sessionToken = sessionToken;
        this.eventId = eventId;
        this.seatIds = seatIds;
        this.orderId = orderId;
    }

    public List<String> getResult() {
        return result;
    }

    public void setResult(List<String> result) {
        this.result = result;
    }
    public String getSessionToken() {
        return sessionToken;
    }
    public String getEventId() {
        return eventId;
    }
    public List<String> getSeatIds() {
        return seatIds;
    }
    public String getOrderId() {
        return orderId;
    }
}
