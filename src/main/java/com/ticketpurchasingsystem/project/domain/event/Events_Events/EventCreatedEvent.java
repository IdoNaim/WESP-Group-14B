package com.ticketpurchasingsystem.project.domain.event.Events_Events;

import com.ticketpurchasingsystem.project.domain.event.Event;

public class EventCreatedEvent {
    private final Event event;

    public EventCreatedEvent(Event event) {
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }
}
