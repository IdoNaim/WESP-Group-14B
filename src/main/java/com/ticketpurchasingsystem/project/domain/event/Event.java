package com.ticketpurchasingsystem.project.domain.event;

import java.time.LocalDateTime;

import jakarta.persistence.*; // Added for JPA annotations

import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingMap;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.AndRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.EventPurchasePolicy;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.MaxAgeRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.MaxTicketsRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.MinAgeRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.MinTicketsRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.OrRule;
import com.ticketpurchasingsystem.project.domain.tickets.ITicketPurchaseRule;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Automatically generates a UUID for new events
    @Column(name = "event_id", updatable = false, nullable = false)
    private String eventId;

    @Column(name = "company_id", nullable = false)
    private int companyId;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "event_capacity", nullable = false)
    private int eventCapacity;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "location")
    private String location; // Merged 'location' and 'eventLocation' into one field

    @Column(name = "image_url")
    private String imageUrl;

    @Version // Tells Hibernate to use this field for Optimistic Locking (concurrency control)
    @Column(name = "version")
    private int version = 0;

    // --- COMPLEX DOMAIN OBJECTS ---
    // Note: I marked these as @Transient so Hibernate ignores them for now.
    // Depending on your DB schema, these either need to be serialized to JSON (@JdbcTypeCode),
    // mapped as @Embedded, or mapped as @OneToOne / @OneToMany relations.
    @Transient
    private SeatingMap seatingMap;

    @Transient
    private EventDiscountPolicy discountPolicy;

    @Transient
    private EventPurchasePolicy purchasePolicy;

    @Transient
    private ITicketPurchaseRule ticketPurchasePolicy;


    // ---------------- CONSTRUCTORS ----------------

    // JPA requires a protected no-args constructor
    protected Event() {}

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

    public Event(
            int companyId,
            String eventName,
            int eventCapacity,
            LocalDateTime eventDate,
            EventPurchasePolicy purchasePolicy,
            EventDiscountPolicy discountPolicy,
            int version
    ) {
        this(companyId, eventName, eventCapacity, eventDate, null, purchasePolicy, discountPolicy, version);
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
        this.imageUrl = other.imageUrl;
    }

    // ---------------- GETTERS & SETTERS ----------------

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public int getCompanyId() { return companyId; }

    public String getEventName() { return eventName; }

    public int getEventCapacity() { return eventCapacity; }
    public void setEventCapacity(int eventCapacity) { this.eventCapacity = eventCapacity; }

    public boolean isActive() { return isActive; }

    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }


    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public SeatingMap getSeatingMap() { return seatingMap; }
    public void setSeatingMap(SeatingMap seatingMap) { this.seatingMap = seatingMap; }

    public ITicketPurchaseRule getTicketPurchasePolicy() { return ticketPurchasePolicy; }
    public void setTicketPurchasePolicy(ITicketPurchaseRule ticketPurchasePolicy) { this.ticketPurchasePolicy = ticketPurchasePolicy; }

    public EventPurchasePolicy getPurchasePolicy() { return purchasePolicy; }

    // ---------------- BUSINESS LOGIC ----------------

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