package com.ticketpurchasingsystem.project.application;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.event.*;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.*;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

@Service
public class EventService implements IEventService {

    private final IEventRepo eventRepo;
    private final EventAggregatePublisher eventPublisher;
    private final EventAggregateListener eventListener;

    // Logger instance
    private final loggerDef logger = loggerDef.getInstance();

    public EventService(IEventRepo eventRepo,
                        EventAggregatePublisher eventPublisher,
                        EventAggregateListener eventListener) {

        this.eventRepo = eventRepo;
        this.eventPublisher = eventPublisher;
        this.eventListener = eventListener;

        logger.info("EventService initialized");
    }

    public boolean createEvent(EventDTO eventDTO,
                               PurchasePolicyDTO purchasePolicyDTO,
                               List<DiscountDTO> discountPolicyDTO) {

        logger.info("Creating event: " + eventDTO.eventName());

        // --- VALIDATION LAYER ---
        if (!purchasePolicyDTO.isQuantityOr() && purchasePolicyDTO.minTickets() != null && purchasePolicyDTO.maxTickets() != null
                && purchasePolicyDTO.minTickets() > purchasePolicyDTO.maxTickets()) {
            logger.error("Failed to create event: minTickets cannot be greater than maxTickets in an AND condition");
            return false;
        }
        if (!purchasePolicyDTO.isAgeOr() && purchasePolicyDTO.minAge() != null && purchasePolicyDTO.maxAge() != null
                && purchasePolicyDTO.minAge() > purchasePolicyDTO.maxAge()) {
            logger.error("Failed to create event: minAge cannot be greater than maxAge in an AND condition");
            return false;
        }

        // --- COMPOSITE RULE CONSTRUCTION ---
        EventPurchasePolicy purchasePolicy = new EventPurchasePolicy();

        // 1. Build Quantity Block
        IPurchaseRule quantityRule = null;
        IPurchaseRule minTkts = purchasePolicyDTO.minTickets() != null ? new MinTicketsRule(purchasePolicyDTO.minTickets()) : null;
        IPurchaseRule maxTkts = purchasePolicyDTO.maxTickets() != null ? new MaxTicketsRule(purchasePolicyDTO.maxTickets()) : null;

        if (minTkts != null && maxTkts != null) {
            quantityRule = purchasePolicyDTO.isQuantityOr() ? new OrRule(minTkts, maxTkts) : new AndRule(minTkts, maxTkts);
        } else {
            quantityRule = (minTkts != null) ? minTkts : maxTkts;
        }

        // 2. Build Age Block
        IPurchaseRule ageRule = null;
        IPurchaseRule minAge = purchasePolicyDTO.minAge() != null ? new MinAgeRule(purchasePolicyDTO.minAge()) : null;
        IPurchaseRule maxAge = purchasePolicyDTO.maxAge() != null ? new MaxAgeRule(purchasePolicyDTO.maxAge()) : null;

        if (minAge != null && maxAge != null) {
            ageRule = purchasePolicyDTO.isAgeOr() ? new OrRule(minAge, maxAge) : new AndRule(minAge, maxAge);
        } else {
            ageRule = (minAge != null) ? minAge : maxAge;
        }

        // 3. Outer Connection: If OR is requested, wrap them.
        // Otherwise, add them as separate flat rules to the policy!
        if (purchasePolicyDTO.isAgeAndQuantityOr() && quantityRule != null && ageRule != null) {
            purchasePolicy.addRule(new OrRule(ageRule, quantityRule));
        } else {
            // No explicit logical wrapper needed! They are added flatly to the checklist.
            if (quantityRule != null) {
                purchasePolicy.addRule(quantityRule);
            }
            if (ageRule != null) {
                purchasePolicy.addRule(ageRule);
            }
        }

        // --- DOMAIN CREATION & PERSISTENCE ---
        EventDiscountPolicy discountPolicy = new EventDiscountPolicy(discountPolicyDTO);

        Event event = new Event(
                eventDTO.companyId(),
                eventDTO.eventName(),
                eventDTO.eventCapacity(),
                eventDTO.eventDateTime(),
                purchasePolicy,
                discountPolicy,
                0
        );

        try {
            Event savedEvent = eventRepo.save(event);
            eventPublisher.publishEventCreated(savedEvent);
            logger.info("Event created successfully: " + eventDTO.eventName());
            return true;
        } catch (Exception e) {
            logger.error("Failed to create event: " + eventDTO.eventName() + " | Error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public EventDTO searchEvent(String eventId) {

        logger.info("Searching for event with ID: " + eventId);

        Event event = eventRepo.findById(eventId);

        if (event == null) {
            logger.warn("Event not found with ID: " + eventId);
            return null;
        }

        logger.info("Event found: " + event.getEventName());

        return new EventDTO(
                event.getCompanyId(),
                event.getEventName(),
                event.getEventCapacity(),
                event.getEventDate(),
                event.isActive()
        );
    }

    @Override
    public List<EventDTO> searchEventsByCompany(int companyId) {

        logger.info("Searching events for company ID: " + companyId);

        List<EventDTO> events = eventRepo.findByCompanyId(companyId)
                .stream()
                .map(event -> new EventDTO(
                        event.getCompanyId(),
                        event.getEventName(),
                        event.getEventCapacity(),
                        event.getEventDate(),
                        event.isActive()
                ))
                .toList();

        logger.info("Found " + events.size() + " events for company ID: " + companyId);

        return events;
    }

    @Override
    public boolean editEventDate(String eventId, LocalDateTime newDateTime) {

        logger.info("Editing date for event ID: " + eventId);

        try {
            Event event = eventRepo.findById(eventId);

            if (event == null) {
                logger.warn("Cannot edit date. Event not found: " + eventId);
                return false;
            }

            event.setEventDate(newDateTime);
            eventRepo.save(event);

            logger.info("Successfully updated date for event ID: " + eventId);
            return true;

        } catch (Exception e) {
            logger.error("Failed to edit event date for ID: "
                    + eventId
                    + " | Error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean removeEvent(String eventId) {

        logger.info("Removing event ID: " + eventId);

        try {
            Event event = eventRepo.findById(eventId);

            if (event == null) {
                logger.warn("Cannot remove event. Event not found: " + eventId);
                return false;
            }

            eventRepo.delete(eventId);

            logger.info("Successfully removed event ID: " + eventId);
            return true;

        } catch (Exception e) {
            logger.error("Failed to remove event ID: "
                    + eventId
                    + " | Error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean editEventInventory(String eventId, int newCapacity) {

        logger.info("Updating capacity for event ID: "
                + eventId
                + " to "
                + newCapacity);

        try {
            Event event = eventRepo.findById(eventId);

            if (event == null) {
                logger.warn("Cannot update capacity. Event not found: " + eventId);
                return false;
            }

            event.setEventCapacity(newCapacity);
            eventRepo.save(event);

            // Publish the EventCapacityChangedEvent for other domains to listen to
            eventPublisher.publishCapacityChanged(eventId, newCapacity);

            logger.info("Successfully updated capacity for event ID: " + eventId);
            return true;

        } catch (Exception e) {
            logger.error("Failed to update capacity for event ID: "
                    + eventId
                    + " | Error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean editEventSeatingMap(String eventId, SeatingMap seatingMap) {

        logger.info("Updating seating map for event ID: " + eventId);

        try {
            Event event = eventRepo.findById(eventId);

            if (event == null) {
                logger.warn("Cannot update seating map. Event not found: " + eventId);
                return false;
            }

            event.setSeatingMap(seatingMap);
            eventRepo.save(event);

            logger.info("Successfully updated seating map for event ID: " + eventId);
            return true;

        } catch (Exception e) {
            logger.error("Failed to update seating map for event ID: "
                    + eventId
                    + " | Error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public SeatingMap configureSeatingMap(List<SeatingAreaConfig> seatingAreas,
                                          List<StandingAreaConfig> standingAreas) {

        logger.info("Configuring seating map");

        SeatingMap seatingMap = new SeatingMap();

        for (SeatingAreaConfig seatingConfig : seatingAreas) {
            seatingMap.addSeatingArea(
                    seatingConfig.getRows(),
                    seatingConfig.getseatsPerRow(),
                    seatingConfig.getPrice()
            );
        }

        for (StandingAreaConfig standingAreaConfig : standingAreas) {
            seatingMap.addStandingArea(
                    standingAreaConfig.getCapacity(),
                    standingAreaConfig.getPrice()
            );
        }

        logger.info("Seating map configured successfully");
        return seatingMap;
    }
    public void releaseSeats(String sessionToken, String orderId, String eventId, List<String> seatIds){
        //TODO: Implement the logic to release reserved seats based on the orderId, eventId, and seatIds
    }
     public void releaseStandingArea(String sessionToken, String eventId, String areaID, int quantity){
        //TODO: Implement the logic to release reserved standing area based on the eventId, areaId, and quantity
     }
     public boolean reserveSeats(String sessionToken, String orderId, String eventId, List<String> seatIds){
        //TODO: Implement the logic to reserve seats based on the orderId, eventId, and seatIds
            return false;
     }
     public boolean reserveStandingArea(String sessionToken, String eventId, String areaId, int quantity){
        //TODO: Implement the logic to reserve standing area based on the eventId, areaId, and quantity
            return false;
     }
}