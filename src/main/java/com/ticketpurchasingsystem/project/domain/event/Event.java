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
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.AndRule;
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
    private String location;

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
            String location,
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
        this.location = location;
    }

    // COPY CONSTRUCTOR: Required for Event::new to work in your Streams
    public Event(Event other) {
        this.eventId = other.eventId;
        this.companyId = other.companyId;
        this.eventName = other.eventName;
        this.eventCapacity = other.eventCapacity;
        this.isActive = other.isActive;
        this.eventDate = other.eventDate;
        this.location = other.location;
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

    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
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

    public void setPurchasePolicy(PurchasePolicyDTO dto) {
        EventPurchasePolicy policy = new EventPurchasePolicy();

        // Build age block (null-safe: only create if at least one age rule exists)
        IPurchaseRule ageBlock = null;
        if (dto.minAge() != null && dto.maxAge() != null) {
            IPurchaseRule minAge = new MinAgeRule(dto.minAge());
            IPurchaseRule maxAge = new MaxAgeRule(dto.maxAge());
            ageBlock = dto.isAgeOr() ? new OrRule(minAge, maxAge) : new AndRule(minAge, maxAge);
        } else if (dto.minAge() != null) {
            ageBlock = new MinAgeRule(dto.minAge());
        } else if (dto.maxAge() != null) {
            ageBlock = new MaxAgeRule(dto.maxAge());
        }

        // Build quantity block
        IPurchaseRule quantityBlock = null;
        if (dto.minTickets() != null && dto.maxTickets() != null) {
            IPurchaseRule minTickets = new MinTicketsRule(dto.minTickets());
            IPurchaseRule maxTickets = new MaxTicketsRule(dto.maxTickets());
            quantityBlock = dto.isQuantityOr() ? new OrRule(minTickets, maxTickets) : new AndRule(minTickets, maxTickets);
        } else if (dto.minTickets() != null) {
            quantityBlock = new MinTicketsRule(dto.minTickets());
        } else if (dto.maxTickets() != null) {
            quantityBlock = new MaxTicketsRule(dto.maxTickets());
        }

        // Combine age block and quantity block into the root rule
        if (ageBlock != null && quantityBlock != null) {
            IPurchaseRule root = dto.isAgeAndQuantityOr()
                    ? new OrRule(ageBlock, quantityBlock)
                    : new AndRule(ageBlock, quantityBlock);
            policy.addRule(root);
        } else if (ageBlock != null) {
            policy.addRule(ageBlock);
        } else if (quantityBlock != null) {
            policy.addRule(quantityBlock);
        }

        this.purchasePolicy = policy;
    }
}