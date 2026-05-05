package com.ticketpurchasingsystem.project.domain.event.Events_Events;

public class EventCapacityChangedEvent {
    private final String eventId;
    private final int newCapacity;

    public EventCapacityChangedEvent(String eventId, int newCapacity) {
        this.eventId = eventId;
        this.newCapacity = newCapacity;
    }

    public String getEventId() {
        return eventId;
    }

    public int getNewCapacity() {
        return newCapacity;
    }
}