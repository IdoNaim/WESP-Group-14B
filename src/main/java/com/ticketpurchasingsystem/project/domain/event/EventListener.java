package com.ticketpurchasingsystem.project.domain.event;

public class EventListener {
    private static EventListener instance;
    public static EventListener getInstance() {
        if (instance == null) {
            instance = new EventListener();
        }
        return instance;
    }
    
}
