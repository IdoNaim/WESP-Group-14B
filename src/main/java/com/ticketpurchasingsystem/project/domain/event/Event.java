package com.ticketpurchasingsystem.project.domain.event;

import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.AndRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.MaxAgeRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.MaxTicketsRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.MinAgeRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.MinTicketsRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.OrRule;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingMap;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.EventPurchasePolicy;

import java.time.LocalDateTime;

import com.ticketpurchasingsystem.project.domain.tickets.ITicketPurchaseRule;

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

    private ITicketPurchaseRule ticketPurchasePolicy;

    private String eventLocation;

    private Double ticketPrice;

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
        this.ticketPurchasePolicy = other.ticketPurchasePolicy;
        this.eventLocation = other.eventLocation;
        this.ticketPrice = other.ticketPrice;
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

    public String getEventLocation() { return eventLocation; }
    public void setEventLocation(String eventLocation) { this.eventLocation = eventLocation; }

    public Double getTicketPrice() { return ticketPrice; }
    public void setTicketPrice(Double ticketPrice) { this.ticketPrice = ticketPrice; }

    public ITicketPurchaseRule getTicketPurchasePolicy() {
        return ticketPurchasePolicy;
    }

    public void setTicketPurchasePolicy(ITicketPurchaseRule ticketPurchasePolicy) {
        this.ticketPurchasePolicy = ticketPurchasePolicy;
    }

    public EventPurchasePolicy getPurchasePolicy() {
        return purchasePolicy;
    }

    public void setPurchasePolicy(PurchasePolicyDTO purchasePolicyDTO){
        EventPurchasePolicy purchasePolicy = new EventPurchasePolicy();

        // Build age sub-rule respecting isAgeOr flag
        IPurchaseRule ageRule = null;
        if (purchasePolicyDTO.minAge() != null && purchasePolicyDTO.maxAge() != null) {
            IPurchaseRule minA = new MinAgeRule(purchasePolicyDTO.minAge());
            IPurchaseRule maxA = new MaxAgeRule(purchasePolicyDTO.maxAge());
            ageRule = purchasePolicyDTO.isAgeOr() ? new OrRule(minA, maxA) : new AndRule(minA, maxA);
        } else if (purchasePolicyDTO.minAge() != null) {
            ageRule = new MinAgeRule(purchasePolicyDTO.minAge());
        } else if (purchasePolicyDTO.maxAge() != null) {
            ageRule = new MaxAgeRule(purchasePolicyDTO.maxAge());
        }

        // Build quantity sub-rule respecting isQuantityOr flag
        IPurchaseRule quantityRule = null;
        if (purchasePolicyDTO.minTickets() != null && purchasePolicyDTO.maxTickets() != null) {
            IPurchaseRule minT = new MinTicketsRule(purchasePolicyDTO.minTickets());
            IPurchaseRule maxT = new MaxTicketsRule(purchasePolicyDTO.maxTickets());
            quantityRule = purchasePolicyDTO.isQuantityOr() ? new OrRule(minT, maxT) : new AndRule(minT, maxT);
        } else if (purchasePolicyDTO.minTickets() != null) {
            quantityRule = new MinTicketsRule(purchasePolicyDTO.minTickets());
        } else if (purchasePolicyDTO.maxTickets() != null) {
            quantityRule = new MaxTicketsRule(purchasePolicyDTO.maxTickets());
        }

        // Combine age and quantity rules respecting isAgeAndQuantityOr flag
        if (ageRule != null && quantityRule != null) {
            IPurchaseRule combined = purchasePolicyDTO.isAgeAndQuantityOr()
                    ? new OrRule(ageRule, quantityRule)
                    : new AndRule(ageRule, quantityRule);
            purchasePolicy.addRule(combined);
        } else if (ageRule != null) {
            purchasePolicy.addRule(ageRule);
        } else if (quantityRule != null) {
            purchasePolicy.addRule(quantityRule);
        }

        this.purchasePolicy = purchasePolicy;
    }
}