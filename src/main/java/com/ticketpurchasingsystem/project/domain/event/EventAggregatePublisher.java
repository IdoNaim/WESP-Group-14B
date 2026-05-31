package com.ticketpurchasingsystem.project.domain.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

// Import your custom events
import com.ticketpurchasingsystem.project.domain.event.Events_Events.EventCancelledEvent;
import com.ticketpurchasingsystem.project.domain.event.Events_Events.EventCapacityChangedEvent;
import com.ticketpurchasingsystem.project.domain.event.Events_Events.EventCreatedEvent;

@Component
public class EventAggregatePublisher {

    private final ApplicationEventPublisher eventPublisher;

    // Spring will automatically inject the publisher here via the constructor
    public EventAggregatePublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishEventCreated(Event newEvent) {
        eventPublisher.publishEvent(new EventCreatedEvent(newEvent));
    }

    public void publishCapacityChanged(String eventId, int newCapacity) {
        eventPublisher.publishEvent(new EventCapacityChangedEvent(eventId, newCapacity));
    }

    public void publishEventCancelled(String eventId, String eventName) {
        eventPublisher.publishEvent(new EventCancelledEvent(eventId, eventName));
    }
}