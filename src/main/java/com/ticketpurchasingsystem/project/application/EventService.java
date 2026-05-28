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
    @Override
    public boolean reserveSeats(String sessionToken, String orderId, String eventId, List<String> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) return true;
        logger.info("Reserving " + seatIds.size() + " seat(s) for event: " + eventId + ", orderId: " + orderId);
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.error("Reserve seats failed: event not found: " + eventId);
            return false;
        }
        SeatingMap seatingMap = event.getSeatingMap();
        if (seatingMap == null) {
            logger.error("Reserve seats failed: event has no seating map: " + eventId);
            return false;
        }
        try {
            return seatingMap.bookAssignedSeats(seatIds, orderId);
        } catch (IllegalStateException e) {
            logger.error("Reserve seats failed for event " + eventId + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean reserveStandingArea(String sessionToken, String eventId, String areaId, int quantity) {
        if (quantity <= 0) return true;
        logger.info("Reserving " + quantity + " standing ticket(s) in area " + areaId + " for event: " + eventId);
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.error("Reserve standing area failed: event not found: " + eventId);
            return false;
        }
        SeatingMap seatingMap = event.getSeatingMap();
        if (seatingMap == null) {
            logger.error("Reserve standing area failed: event has no seating map: " + eventId);
            return false;
        }
        boolean result = seatingMap.bookStandingArea(areaId, null, quantity);
        if (!result) {
            logger.error("Reserve standing area failed: insufficient capacity in area " + areaId + " for event " + eventId);
        }
        return result;
    }

    @Override
    public void releaseSeats(String sessionToken, String orderId, String eventId, List<String> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) return;
        logger.info("Releasing " + seatIds.size() + " seat(s) for event: " + eventId + ", orderId: " + orderId);
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.warn("Release seats: event not found: " + eventId);
            return;
        }
        SeatingMap seatingMap = event.getSeatingMap();
        if (seatingMap == null) {
            logger.warn("Release seats: event has no seating map: " + eventId);
            return;
        }
        try {
            seatingMap.unbookAssignedSeats(seatIds);
        } catch (Exception e) {
            logger.error("Release seats failed for event " + eventId + ": " + e.getMessage());
        }
    }

    @Override
    public void releaseStandingArea(String sessionToken, String eventId, String areaID, int quantity) {
        if (quantity <= 0) return;
        logger.info("Releasing " + quantity + " standing ticket(s) from area " + areaID + " for event: " + eventId);
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.warn("Release standing area: event not found: " + eventId);
            return;
        }
        SeatingMap seatingMap = event.getSeatingMap();
        if (seatingMap == null) {
            logger.warn("Release standing area: event has no seating map: " + eventId);
            return;
        }
        boolean released = seatingMap.unbookStandingArea(areaID, quantity);
        if (!released) {
            logger.warn("Release standing area failed for area " + areaID + " in event " + eventId);
        }
    }

    @Override
    public boolean checkSeatAvailability(String eventId, List<String> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) return true;
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.error("checkSeatAvailability: event not found: " + eventId);
            return false;
        }
        SeatingMap seatingMap = event.getSeatingMap();
        if (seatingMap == null) {
            logger.error("checkSeatAvailability: event has no seating map: " + eventId);
            return false;
        }
        for (String seatId : seatIds) {
            Bookable area = seatingMap.getArea(seatId);
            if (area == null) {
                logger.warn("checkSeatAvailability: seat not found: " + seatId);
                return false;
            }
            if (!(area instanceof AssignedSeat)) {
                logger.warn("checkSeatAvailability: " + seatId + " is not an assigned seat");
                return false;
            }
            if (((AssignedSeat) area).isBooked()) {
                logger.info("checkSeatAvailability: seat already booked: " + seatId);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean checkStandingAreaAvailability(String eventId, String areaId, int quantity) {
        if (quantity <= 0) return true;
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.error("checkStandingAreaAvailability: event not found: " + eventId);
            return false;
        }
        SeatingMap seatingMap = event.getSeatingMap();
        if (seatingMap == null) {
            logger.error("checkStandingAreaAvailability: event has no seating map: " + eventId);
            return false;
        }
        Bookable area = seatingMap.getArea(areaId);
        if (area == null) {
            logger.warn("checkStandingAreaAvailability: area not found: " + areaId);
            return false;
        }
        if (!(area instanceof StandingArea)) {
            logger.warn("checkStandingAreaAvailability: " + areaId + " is not a standing area");
            return false;
        }
        int available = ((StandingArea) area).getAvalibleSeatNumber();
        if (available < quantity) {
            logger.info("checkStandingAreaAvailability: only " + available + " spots available, requested " + quantity);
            return false;
        }
        return true;
    }
}