package com.ticketpurchasingsystem.project.domain.event;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Event {

    private Integer eventId;

    private  int companyId;
    private String eventName;

    private int eventCapacity;

    private boolean isActive;

    private LocalDateTime eventDate;

    //private SeatingMap seatingMap;

    private EventDiscountPolicy discountPolicy;

    private EventPurchasePolicy purchasePolicy;

    private final transient List<Object> domainEvents = new ArrayList<>();


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


    // ---------------- BUSINESS LOGIC ----------------

    public boolean isAvailable() {
        return isActive && eventCapacity > 0;
    }


    public double calculatePriceForUser(
            PurchaseContext context,
            double basePrice
    ) {

        validatePurchase(context);

        double finalPrice = basePrice;

        if (discountPolicy != null) {

            // double discountPercentage =
            //         discountPolicy.calculateDiscount(
            //                 context.getUserGroups()
            //         );
            double discountPercentage = 1;
            finalPrice =
                    basePrice - (basePrice * discountPercentage / 100);
        }

        return finalPrice;
    }


    public void reserveTickets(PurchaseContext context) {

        if (!isAvailable()) {
            throw new IllegalStateException(
                    "Event inactive or sold out"
            );
        }

        validatePurchase(context);

        if (eventCapacity < context.getTicketAmount()) {
            throw new IllegalStateException(
                    "Not enough tickets available"
            );
        }

        eventCapacity -= context.getTicketAmount();
    }


    private void validatePurchase(PurchaseContext context) {

        if (purchasePolicy != null) {
            purchasePolicy.validatePurchase(context);
        }
    }


    // ---------------- EVENT MANAGEMENT ----------------

    public void updateInventory(int newCapacity) {

        if (newCapacity < 0) {
            throw new IllegalArgumentException(
                    "Capacity cannot be negative"
            );
        }

        if (newCapacity < this.eventCapacity) {
            throw new IllegalStateException(
                    "Cannot reduce capacity below current reserved tickets"
            );
        }

        this.eventCapacity = newCapacity;
    }


    public void updateEventDate(LocalDateTime newDate) {

        if (newDate == null) {
            throw new IllegalArgumentException(
                    "Event date cannot be null"
            );
        }

        this.eventDate = newDate;
    }


    public void configureSeatingMap(String seatingMap) {

        if (seatingMap == null) {
            throw new IllegalArgumentException(
                    "Seating map cannot be null"
            );
        }

        //this.seatingMap = seatingMap;
    }


    public void activate() {
        this.isActive = true;
    }


    public void deactivate() {
        this.isActive = false;
    }


    public void updatePurchasePolicy(
            EventPurchasePolicy purchasePolicy
    ) {
        this.purchasePolicy = purchasePolicy;
    }


    public void updateDiscountPolicy(
            EventDiscountPolicy discountPolicy
    ) {
        this.discountPolicy = discountPolicy;
    }


    // ---------------- DOMAIN EVENTS ----------------

    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }


    public void clearDomainEvents() {
        domainEvents.clear();
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


    public String getSeatingMap() {
        return "";
    }
}