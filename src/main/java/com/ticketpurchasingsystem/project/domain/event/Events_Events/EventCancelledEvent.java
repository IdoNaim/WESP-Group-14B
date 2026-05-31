package com.ticketpurchasingsystem.project.domain.event.Events_Events;

public class EventCancelledEvent {
    private final String eventId;
    private final String eventName;

    public EventCancelledEvent(String eventId, String eventName) {
        this.eventId = eventId;
        this.eventName = eventName;
    }

    public String getEventId() { return eventId; }
    public String getEventName() { return eventName; }
}
