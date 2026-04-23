package com.ticketpurchasingsystem.project.domain.event;

/**
 * EventPurchasePolicy is a Value Object belonging to the Event aggregate.
 * It uses a composite of rules to validate a purchase attempt, making it 
 * flexible for both production companies and specific events.
 */
public class EventPurchasePolicy {
    private Integer minTickets;
    private Integer maxTickets;   
    private Integer minAge;
    private Integer maxAge;
    private boolean canLeaveEmptySeats;
    // Add more rules as needed, e.g., allowed routes, time-based restrictions, etc.

    public EventPurchasePolicy(Integer minTickets, Integer maxTickets, Integer minAge, Integer maxAge, boolean emptySeatLeft) {
        if (minTickets != null && maxTickets != null && minTickets > maxTickets) {
            throw new IllegalArgumentException("minTickets cannot be greater than maxTickets");
        }
        if (minAge != null && maxAge != null && minAge > maxAge) {
            throw new IllegalArgumentException("minAge cannot be greater than maxAge");
        }
        this.minTickets = minTickets;
        this.maxTickets = maxTickets;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.canLeaveEmptySeats = emptySeatLeft;
    }

    public boolean canPurchase(int requestedTickets, int buyerAge, boolean isSeatEmpty) {
        if (minTickets != null && requestedTickets < minTickets) {
            return false;
        }
        if (maxTickets != null && requestedTickets > maxTickets) {
            return false;
        }
        if (minAge != null && buyerAge < minAge) {
            return false;
        }
        if (maxAge != null && buyerAge > maxAge) {
            return false;
        }
        if(canLeaveEmptySeats && !isSeatEmpty) {
            return false;
        }
        //Add more rule checks as needed
        return true;
    }
}