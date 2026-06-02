package com.ticketpurchasingsystem.project.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.event.*;
import com.ticketpurchasingsystem.project.domain.event.Maps.AssignedSeat;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingAreaConfig;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingMap;
import com.ticketpurchasingsystem.project.domain.event.Maps.StandingArea;
import com.ticketpurchasingsystem.project.domain.event.Maps.StandingAreaConfig;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.*;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

@Service
public class EventService implements IEventService {

    private final IEventRepo eventRepo;
    private final EventAggregatePublisher eventPublisher;
    private final AuthenticationService authenticationService;

    // Logger instance
    private final loggerDef logger = loggerDef.getInstance();

    public EventService(IEventRepo eventRepo,
                        EventAggregatePublisher eventPublisher,
                        AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
        this.eventRepo = eventRepo;
        this.eventPublisher = eventPublisher;

        logger.info("EventService initialized");
    }

    public boolean createEvent(String sessionToken, EventDTO eventDTO,
                               PurchasePolicyDTO purchasePolicyDTO,
                               List<DiscountDTO> discountPolicyDTO) {
        if(!authenticationService.validate(sessionToken)) {
            throw new IllegalArgumentException("Invalid session token");
        }
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
    public EventDTO searchEvent(String sessionToken, String eventId) {
        if(!authenticationService.validate(sessionToken)) {
            throw new IllegalArgumentException("Invalid session token");
        }

        logger.info("Searching for event with ID: " + eventId);

        Event event = eventRepo.findById(eventId);

        if (event == null) {
            logger.warn("Event not found with ID: " + eventId);
            return null;
        }

        logger.info("Event found: " + event.getEventName());

        return new EventDTO(
                event.getEventId(),
                event.getCompanyId(),
                event.getEventName(),
                event.getEventCapacity(),
                event.getEventDate(),
                event.isActive()
        );
    }

    @Override
    public List<EventDTO> searchEventsByCompany(String sessionToke, int companyId) {
        if(!authenticationService.validate(sessionToke)) {
            throw new IllegalArgumentException("Invalid session token");
        }

        logger.info("Searching events for company ID: " + companyId);

        List<EventDTO> events = eventRepo.findByCompanyId(companyId)
                .stream()
                .map(event -> new EventDTO(
                        event.getEventId(),
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
    public boolean editEventDate(String sessionToken, String eventId, LocalDateTime newDateTime) {
        if(!authenticationService.validate(sessionToken)) {
            throw new IllegalArgumentException("Invalid session token");
        }

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
    public boolean removeEvent(String sessionToken, String eventId) {
        if(!authenticationService.validate(sessionToken)) {
            throw new IllegalArgumentException("Invalid session token");
        }

        logger.info("Removing event ID: " + eventId);

        try {
            Event event = eventRepo.findById(eventId);

            if (event == null) {
                logger.warn("Cannot remove event. Event not found: " + eventId);
                return false;
            }

            String eventName = event.getEventName();
            eventPublisher.publishEventCancelled(eventId, eventName);
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
    public boolean editEventInventory(String sessionToken, String eventId, int newCapacity) {
        if(!authenticationService.validate(sessionToken)) {
            throw new IllegalArgumentException("Invalid session token");
        }

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
    public boolean editEventSeatingMap(String sesionToken, String eventId, SeatingMap seatingMap) {
        if(!authenticationService.validate(sesionToken)) {
            throw new IllegalArgumentException("Invalid session token");
        }

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
    public SeatingMap configureSeatingMap(String sessionToken, List<SeatingAreaConfig> seatingAreas,
                                          List<StandingAreaConfig> standingAreas) {
        if(!authenticationService.validate(sessionToken)) {
            throw new IllegalArgumentException("Invalid session token");
        }

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
    public void releaseSeats(String sessionToken, String orderId, String eventId, List<String> seatIds) {
        //TODO: Implement the logic to release reserved seats based on the orderId, eventId, and seatIds
        if (!authenticationService.validate(sessionToken)) {
            throw new IllegalArgumentException("Invalid session token");
        }
        logger.info("Releasing seats");
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.warn("Cannot release seats. Event not found: " + eventId);
            throw new IllegalArgumentException("Invalid EventID");
        }
        if (!event.getSeatingMap().unbookAssignedSeats(seatIds)) {
            logger.warn("Cannot release seats. one or more seats not booked");
            throw new IllegalArgumentException("one or more seats not booked");
        }
        logger.info("Released seats successfully");
    }
    public void releaseStandingArea(String sessionToken, String eventId, String areaID, int quantity){
        //TODO: Implement the logic to release reserved standing area based on the eventId, areaId, and quantity
        if (!authenticationService.validate(sessionToken)) {
            throw new IllegalArgumentException("Invalid session token");
        }
        logger.info("Releasing standing area");
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.warn("Cannot release standing area. Event not found: " + eventId);
            throw new IllegalArgumentException("Invalid EventID");
        }
        if(!event.getSeatingMap().unbookStandingArea(areaID, quantity)){
            logger.warn("Cannot release standing area. one or more stands not booked");
            throw new IllegalArgumentException("one or more stands not booked");
        }
        logger.info("Released standing area successfully");
    }
    public boolean reserveSeats(String sessionToken, String orderId, String eventId, List<String> seatIds){
        //TODO: Implement the logic to reserve seats based on the orderId, eventId, and seatIds
        if (!authenticationService.validate(sessionToken)) {
            throw new IllegalArgumentException("Invalid session token");
        }
        logger.info("Releasing seats");
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.warn("Cannot release seats. Event not found: " + eventId);
            throw new IllegalArgumentException("Invalid EventID");
        }
        if(!event.getSeatingMap().bookAssignedSeats(seatIds, orderId)){
            logger.warn("Cannot book seats, problem occured");
            throw new IllegalArgumentException("cannot book seats, problem occured");
        }
        logger.info("booked seats successfully");
        return true;
    }
    public boolean reserveStandingArea(String sessionToken, String eventId, String areaId, int quantity){
        //TODO: Implement the logic to reserve standing area based on the eventId, areaId, and quantity
        if (!authenticationService.validate(sessionToken)) {
            throw new IllegalArgumentException("Invalid session token");
        }
        logger.info("booking standing area");
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.warn("Cannot book standing area. Event not found: " + eventId);
            throw new IllegalArgumentException("Invalid EventID");
        }
        if(!event.getSeatingMap().bookStandingArea(areaId, null,quantity)){
            logger.warn("Cannot book standing area. one or more stands not booked");
            throw new IllegalArgumentException("cannot book standing area, problem occured");
        }
        logger.info("booked standing area successfully");
        return true;

    }
    public List<String> checkSeatsReserved(String sessionToken, String orderId, String eventId, List<String> seatIds){
        //checks for each seatid if really saved for this specific orderID, if not, add the seatID to result
        //string list and return it
        if (!authenticationService.validate(sessionToken)) {
            throw new IllegalArgumentException("Invalid session token");
        }
        logger.info("checking reserved seats");
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.warn("Cannot check reserved seats. Event not found: " + eventId);
            throw new IllegalArgumentException("Invalid EventID");
        }
        List<String> reservedSeatIds = new ArrayList<>();
        for (String seatId : seatIds) {
            if (!event.getSeatingMap().getSeat(seatId).isbooked(orderId)) {
                logger.warn("reserved seat " + seatId + " is not booked");
                reservedSeatIds.add(seatId);
            }
        }
        logger.info("returning the unreserved seats");
        return reservedSeatIds;
    }
    @Override
    public boolean editEventPurchasePolicy(String sesssionToken, String eventId, PurchasePolicyDTO purchasePolicyDTO){
        if(!authenticationService.validate(sesssionToken)){
            throw new IllegalArgumentException("Invalid session token");
        }
        Event event = eventRepo.findById(eventId);
        if(event == null){
            logger.warn("Cannot check reserved seats. Event not found: " + eventId);
            throw new RuntimeException("couldnt find event by eventId "+ eventId);
        }
        event.setPurchasePolicy(purchasePolicyDTO);
        try {
            eventRepo.save(event);
            logger.info("changed purchase policy of event with id: " + eventId);
            return true;
        }catch (Exception e){
            logger.error("Failed to edit event purchse policy for ID: "
                    + eventId
                    + " | Error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean checkSeatAvailability(String eventId, List<String> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) return true;
        Event event = eventRepo.findById(eventId);
        if (event == null || event.getSeatingMap() == null) return false;
        SeatingMap map = event.getSeatingMap();
        for (String seatId : seatIds) {
            AssignedSeat seat = map.getSeat(seatId);
            if (seat == null || seat.isBooked()) return false;
        }
        return true;
    }

    @Override
    public boolean checkStandingAreaAvailability(String eventId, String areaId, int quantity) {
        if (quantity <= 0) return false;
        Event event = eventRepo.findById(eventId);
        if (event == null || event.getSeatingMap() == null) return false;
        StandingArea area = event.getSeatingMap().getArea(areaId);
        return area != null && area.getAvalibleSeatNumber() >= quantity;
    }
}
