package com.ticketpurchasingsystem.project.domain.event;

import java.time.LocalDateTime;

public class Event {

    private String eventId;

    private final int companyId;
    private String eventName;

    private int eventCapacity;

    private boolean isActive;

    private LocalDateTime eventDate;

    private SeatingMap seatingMap;

    private EventDiscountPolicy discountPolicy;

    private EventPurchasePolicy purchasePolicy;

    private int version = 0;


    public Event(
            int companyId,
            String eventName,
            int eventCapacity,
            LocalDateTime eventDate,
            EventPurchasePolicy purchasePolicy,
            EventDiscountPolicy discountPolicy,
            int version
    ) {

        if (eventName == null || eventName.trim().isEmpty()) {
            throw new IllegalArgumentException("Event name cannot be empty");
        }

        if (eventCapacity <= 0) {
            throw new IllegalArgumentException("Event capacity must be greater than 0");
        }

        if (eventDate == null) {
            throw new IllegalArgumentException("Event date cannot be null");
        }

        this.companyId = companyId;
        this.eventName = eventName;
        this.eventCapacity = eventCapacity;
        this.eventDate = eventDate;
        this.purchasePolicy = purchasePolicy;
        this.discountPolicy = discountPolicy;
        this.isActive = true;
        this.version = version;
    }

    // COPY CONSTRUCTOR: Required for Event::new to work in your Streams
    public Event(Event other) {
        this.eventId = other.eventId;
        this.companyId = other.companyId;
        this.eventName = other.eventName;
        this.eventCapacity = other.eventCapacity;
        this.isActive = other.isActive;
        this.eventDate = other.eventDate;
        this.seatingMap = other.seatingMap;
        this.discountPolicy = other.discountPolicy;
        this.purchasePolicy = other.purchasePolicy;
        this.version = other.version;
    }


    // ---------------- GETTERS ----------------

    public String getEventId() {
        return eventId;
    }


    public int getCompanyId() {
        return companyId;
    }


    public String getEventName() {
        return eventName;
    }


    public int getEventCapacity() {
        return eventCapacity;
    }

    public int getVersion(){
        return version;
    }


    public boolean isActive() {
        return isActive;
    }


    public LocalDateTime getEventDate() {
        return eventDate;
    }


    public SeatingMap getSeatingMap() {
        return seatingMap;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public void setSeatingMap(SeatingMap seatingMap) {
        this.seatingMap = seatingMap;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setEventCapacity(int eventCapacity) { this.eventCapacity = eventCapacity; }
}