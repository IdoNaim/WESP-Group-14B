package com.ticketpurchasingsystem.project.domain.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

// Import your custom events
import com.ticketpurchasingsystem.project.domain.event.Events_Events.EventCreatedEvent;
import com.ticketpurchasingsystem.project.domain.event.Events_Events.EventCapacityChangedEvent;

@Component
public class EventAggregateListener {

    // You can inject repositories or handlers here if needed, just like in UserListener
    public EventAggregateListener() {
    }

    @EventListener
    public void onEventCreated(EventCreatedEvent event) {
        loggerDef.getInstance().info(
                "New Event created: ID=" + event.getEvent().getEventId() +
                        ", Name=" + event.getEvent().getEventName()
        );
        // Handle logic, notify other aggregates, update read models, etc.
    }

    @EventListener
    public void onCapacityChanged(EventCapacityChangedEvent event) {
        loggerDef.getInstance().info(
                "Event Capacity Updated: ID=" + event.getEventId() +
                        ", New Capacity=" + event.getNewCapacity()
        );
        // Handle logic
    }
}