package com.ticketpurchasingsystem.project.domain.ActiveOrders;

import java.util.concurrent.CompletableFuture;

public class TicketReservationEvent {
    private final String eventId;
    private final int quantity;

    private Boolean result;

    public TicketReservationEvent(String eventId, int quantity) {
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