package com.ticketpurchasingsystem.project.application;

import java.time.LocalDateTime;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.event.*;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.*;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

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
        // Retain the logical checks that used to live in your old value object constructor
        if (purchasePolicyDTO.minTickets() != null && purchasePolicyDTO.maxTickets() != null
                && purchasePolicyDTO.minTickets() > purchasePolicyDTO.maxTickets()) {
            logger.error("Failed to create event: minTickets cannot be greater than maxTickets");
            return false;
        }
        if (purchasePolicyDTO.minAge() != null && purchasePolicyDTO.maxAge() != null
                && purchasePolicyDTO.minAge() > purchasePolicyDTO.maxAge()) {
            logger.error("Failed to create event: minAge cannot be greater than maxAge");
            return false;
        }

        // --- COMPOSITE RULE CONSTRUCTION ---
        // Initialize the composite container
        EventPurchasePolicy purchasePolicy = new EventPurchasePolicy();

        // Dynamically add your rules if they are specified in the DTO
        if (purchasePolicyDTO.minTickets() != null) {
            purchasePolicy.addRule(new MinTicketsRule(purchasePolicyDTO.minTickets()));
        }

        // REUSE: Use your colleague's production rule right here!
        if (purchasePolicyDTO.maxTickets() != null) {
            purchasePolicy.addRule(new MaxTicketsRule(purchasePolicyDTO.maxTickets()));
        }

        if (purchasePolicyDTO.minAge() != null) {
            purchasePolicy.addRule(new MinAgeRule(purchasePolicyDTO.minAge()));
        }

        if (purchasePolicyDTO.maxAge() != null) {
            purchasePolicy.addRule(new MaxAgeRule(purchasePolicyDTO.maxAge()));
        }

        // Preserve your original typo method name 'emnptySeatLeft()' from your DTO
        purchasePolicy.addRule(new EmptySeatRule(purchasePolicyDTO.emnptySeatLeft()));

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
            // Save the event and capture the returned object (which includes the newly generated eventId)
            Event savedEvent = eventRepo.save(event);

            // Publish the EventCreatedEvent for other domains (Notification, Ticket) to listen to
            eventPublisher.publishEventCreated(savedEvent);

            logger.info("Event created successfully: " + eventDTO.eventName());
            return true;

        } catch (Exception e) {
            logger.error("Failed to create event: "
                    + eventDTO.eventName()
                    + " | Error: " + e.getMessage());
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
     //checks for each seatId if its reserved to orderId, and returns a list of seatIds, that arent reserved for this specific orderId.
     public List<String> checkSeatsReserved(String sessionToken, String orderId, String eventId, List<String> seatIds){
        //TODO: Implement the logic to check if seats are reserved based on the orderId, eventId, and seatIds
            return List.of();
     }
}