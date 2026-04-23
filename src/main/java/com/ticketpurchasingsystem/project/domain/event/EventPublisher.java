package com.ticketpurchasingsystem.project.domain.event;

public class EventPublisher {
    private static EventPublisher instance;
    public static EventPublisher getInstance() {
        if (instance == null) {
            instance = new EventPublisher();
        }
        return instance;
    }
    
}
