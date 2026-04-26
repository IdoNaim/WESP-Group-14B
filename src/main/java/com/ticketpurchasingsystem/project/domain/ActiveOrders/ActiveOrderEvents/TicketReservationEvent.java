package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import org.springframework.context.ApplicationEvent;

import java.util.concurrent.CompletableFuture;

public class TicketReservationEvent extends ApplicationEvent {
    private final String eventId;
    private final int quantity;

    private Boolean result;

    public TicketReservationEvent(Object source, String eventId, int quantity) {
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

    public Boolean getResult() {
        return result;
    }
}