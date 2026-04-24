package com.ticketpurchasingsystem.project.domain.event;

import java.time.LocalDateTime;

public class Event {

    private Integer eventId;

    private final int companyId;
    private String eventName;

    private int eventCapacity;

    private boolean isActive;

    private LocalDateTime eventDate;

    private SeatingMap seatingMap;

    private EventDiscountPolicy discountPolicy;

    private EventPurchasePolicy purchasePolicy;


    public Event(
            int companyId,
            String eventName,
            int eventCapacity,
            LocalDateTime eventDate,
            EventPurchasePolicy purchasePolicy,
            EventDiscountPolicy discountPolicy
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
    }


    // ---------------- GETTERS ----------------

    public Integer getEventId() {
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


    public boolean isActive() {
        return isActive;
    }


    public LocalDateTime getEventDate() {
        return eventDate;
    }


    public SeatingMap getSeatingMap() {
        return seatingMap;
    }

    public void setEventId(Integer eventId) {
        this.eventId = eventId;
    }
}