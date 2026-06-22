package com.ticketpurchasingsystem.project.domain.event.Events_Events;

public class EventUpdatedEvent {
    private final String eventId;
    private final String eventName;
    private final String changeDescription;

    public EventUpdatedEvent(String eventId, String eventName, String changeDescription) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.changeDescription = changeDescription;
    }

    public String getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public String getChangeDescription() { return changeDescription; }
}
