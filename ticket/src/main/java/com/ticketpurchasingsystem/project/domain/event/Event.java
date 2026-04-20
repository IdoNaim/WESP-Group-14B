package com.ticketpurchasingsystem.project.domain.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Event Aggregate Root.
 * Manages the state and business rules of an event.
 */
public class Event {
    
    private Integer eventId;
    private int companyId;
    private String eventName;
    private int eventCapacity;
    private boolean isActive;

    private EventDiscountPolicy discountPolicy;
    private EventPurchasePolicy purchasePolicy;

    private final transient List<Object> domainEvents = new ArrayList<>();

    public Event(int companyId, String eventName, int eventCapacity, 
                 EventPurchasePolicy purchasePolicy, EventDiscountPolicy discountPolicy) {
        if (eventName == null || eventName.trim().isEmpty()) {
            throw new IllegalArgumentException("Event name cannot be empty");
        }
        if (eventCapacity <= 0) {
            throw new IllegalArgumentException("Event capacity must be greater than 0");
        }
        
        this.companyId = companyId;
        this.eventName = eventName;
        this.eventCapacity = eventCapacity;
        this.purchasePolicy = purchasePolicy;
        this.discountPolicy = discountPolicy;
        this.isActive = true; // Events are active by default
    }

    public boolean isAvailable() {
        return isActive && eventCapacity > 0;
    }

    public double calculatePriceForUser(PurchaseContext context, double basePrice) {
        if (purchasePolicy != null) {
            purchasePolicy.validatePurchase(context);
        }
        double finalPrice = basePrice;
        if (discountPolicy != null) {
            double discountPercentage = discountPolicy.calculateDiscount(context.getUserGroups());
            finalPrice = basePrice - (basePrice * (discountPercentage / 100));
        }

        return finalPrice;
    }

    public void reserveTickets(PurchaseContext context) {
        if (!isAvailable()) {
            throw new IllegalStateException("Event is currently inactive or sold out.");
        }

        if (purchasePolicy != null) {
            purchasePolicy.validatePurchase(context);
        }

        if (eventCapacity < context.getTicketAmount()) {
            throw new IllegalStateException("Not enough tickets available for this event.");
        }

        this.eventCapacity -= context.getTicketAmount();

    }

    // --- Domain Events Handling ---

    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public int getEventCapacity() { return eventCapacity; }
    public boolean isActive() { return isActive; }
    public String getEventName() { return eventName; }
}