package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import org.springframework.context.ApplicationEvent;

public class TicketUnreservationEvent extends ApplicationEvent {
    private String eventId;
    private int quantity;


    public TicketUnreservationEvent(Object source, String eventId, int quantity) {
        super(source);
        this.eventId = eventId;
        this.quantity = quantity;
    }

    public String getEventId() {
        return eventId;
    }

    public int getAmount() {
        return quantity;
    }
}
