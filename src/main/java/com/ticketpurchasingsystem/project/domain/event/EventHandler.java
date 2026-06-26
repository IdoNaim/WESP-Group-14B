package com.ticketpurchasingsystem.project.domain.event;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.SeatingMapDTO;
import com.ticketpurchasingsystem.project.domain.event.*;
import com.ticketpurchasingsystem.project.domain.event.Maps.AssignedSeat;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingAreaConfig;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingMap;
import com.ticketpurchasingsystem.project.domain.event.Maps.StandingArea;
import com.ticketpurchasingsystem.project.domain.event.Maps.StandingAreaConfig;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.*;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

public class EventHandler {

    private static volatile EventHandler instance;

    private IEventRepo eventRepo;
    private EventAggregatePublisher eventPublisher;
    private AuthenticationService authenticationService;

    private final loggerDef logger = loggerDef.getInstance();

    public EventHandler(IEventRepo eventRepo,
                        EventAggregatePublisher eventPublisher,
                        AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
        this.eventRepo = eventRepo;
        this.eventPublisher = eventPublisher;

        logger.info("EventHandler instance constructed");
    }

    public static EventHandler getInstance(IEventRepo eventRepo,
                                           EventAggregatePublisher eventPublisher,
                                           AuthenticationService authenticationService) {
        if (instance == null) {
            synchronized (EventHandler.class) {
                if (instance == null) {
                    instance = new EventHandler(eventRepo, eventPublisher, authenticationService);
                }
            }
        }

        instance.eventRepo = eventRepo;
        instance.eventPublisher = eventPublisher;
        instance.authenticationService = authenticationService;

        return instance;
    }

    private String extractToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    public String createEvent(String sessionToken, EventDTO eventDTO,
                              PurchasePolicyDTO purchasePolicyDTO,
                              List<DiscountDTO> discountPolicyDTO) {
        if(!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        logger.info("Creating event: " + eventDTO.eventName());

        if (eventDTO.eventDateTime() != null && eventDTO.eventDateTime().isBefore(LocalDateTime.now())) {
            logger.error("Failed to create event: event date cannot be in the past");
            return null;
        }

        if (!purchasePolicyDTO.isQuantityOr() && purchasePolicyDTO.minTickets() != null && purchasePolicyDTO.maxTickets() != null
                && purchasePolicyDTO.minTickets() > purchasePolicyDTO.maxTickets()) {
            logger.error("Failed to create event: minTickets cannot be greater than maxTickets in AND rule");
            return null;
        }
        if (!purchasePolicyDTO.isAgeOr() && purchasePolicyDTO.minAge() != null && purchasePolicyDTO.maxAge() != null
                && purchasePolicyDTO.minAge() > purchasePolicyDTO.maxAge()) {
            logger.error("Failed to create event: minAge cannot be greater than maxAge in AND rule");
            return null;
        }

        EventPurchasePolicy purchasePolicy = new EventPurchasePolicy();
        purchasePolicy.updateFromDTO(purchasePolicyDTO);

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
        event.setLocation(eventDTO.eventLocation());
        event.setImageUrl(eventDTO.imageUrl());

        try {
            event.setSeatingMap(new SeatingMap());
            Event savedEvent = eventRepo.save(event);
            eventPublisher.publishEventCreated(savedEvent);
            logger.info("Event created successfully: " + eventDTO.eventName());
            return savedEvent.getEventId();
        } catch (Exception e) {
            logger.error("Failed to create event: " + eventDTO.eventName() + " | Error: " + e.getMessage());
            return null;
        }
    }

    private EventDTO toDTO(Event event) {
        Double minPrice = null;
        Double maxPrice = null;
        int displayCapacity = event.getEventCapacity();
        if (event.getSeatingMap() != null) {
            java.util.List<Double> prices = event.getSeatingMap().getAllZonePrices();
            if (!prices.isEmpty()) {
                minPrice = prices.stream().mapToDouble(Double::doubleValue).min().getAsDouble();
                maxPrice = prices.stream().mapToDouble(Double::doubleValue).max().getAsDouble();
            }
            int mapCapacity = event.getSeatingMap().getTotalAvailableCapacity();
            if (mapCapacity > 0) displayCapacity = mapCapacity;
        }
        return new EventDTO(
                event.getEventId(),
                event.getCompanyId(),
                event.getEventName(),
                displayCapacity,
                event.getEventDate(),
                event.isActive(),
                event.getLocation(),
                event.getImageUrl(),
                minPrice,
                maxPrice
        );
    }

    public EventDTO searchEvent(String sessionToken, String eventId) {
        if(!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        logger.info("Searching for event with ID: " + eventId);
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.warn("Event not found with ID: " + eventId);
            return null;
        }
        logger.info("Event found: " + event.getEventName());
        return toDTO(event);
    }

    public List<EventDTO> searchEventsByCompany(String sessionToke, int companyId) {
        if(!authenticationService.validate(extractToken(sessionToke))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        logger.info("Searching events for company ID: " + companyId);
        List<EventDTO> events = eventRepo.findByCompanyId(companyId)
                .stream()
                .map(this::toDTO)
                .toList();
        logger.info("Found " + events.size() + " events for company ID: " + companyId);
        return events;
    }

    public List<EventDTO> getAllActiveEvents() {
        logger.info("Fetching all active events");
        return eventRepo.findActiveEvents()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public boolean editEventImage(String sessionToken, String eventId, String newImageUrl) {
        if (!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        try {
            Event event = eventRepo.findById(eventId);
            if (event == null) return false;
            event.setImageUrl(newImageUrl);
            eventRepo.save(event);
            publishUpdateNotification(eventId, event.getEventName(), "The event image has been updated.");
            return true;
        } catch (Exception e) {
            logger.error("Failed to edit event image for ID: " + eventId + " | Error: " + e.getMessage());
            return false;
        }
    }

    public boolean editEventDate(String sessionToken, String eventId, LocalDateTime newDateTime) {
        if(!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        logger.info("Editing date for event ID: " + eventId);
        try {
            Event event = eventRepo.findById(eventId);
            if (event == null) {
                logger.warn("Cannot edit date. Event not found: " + eventId);
                return false;
            }
            if (newDateTime == null || newDateTime.isBefore(LocalDateTime.now())) {
                logger.warn("Cannot edit date: new date cannot be in the past");
                return false;
            }
            event.setEventDate(newDateTime);
            eventRepo.save(event);
            publishUpdateNotification(eventId, event.getEventName(),
                    "The date of this event has been updated to " + newDateTime + ".");
            logger.info("Successfully updated date for event ID: " + eventId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to edit event date for ID: " + eventId + " | Error: " + e.getMessage());
            return false;
        }
    }

    public boolean removeEvent(String sessionToken, String eventId) {
        if(!authenticationService.validate(extractToken(sessionToken))) {
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
            event.cancel();
            eventRepo.save(event);
            logger.info("Successfully cancelled event ID: " + eventId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to remove event ID: " + eventId + " | Error: " + e.getMessage());
            return false;
        }
    }

    public boolean editEventInventory(String sessionToken, String eventId, int newCapacity) {
        if(!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        logger.info("Updating capacity for event ID: " + eventId + " to " + newCapacity);
        try {
            Event event = eventRepo.findById(eventId);
            if (event == null) {
                logger.warn("Cannot update capacity. Event not found: " + eventId);
                return false;
            }
            if (newCapacity < event.getEventCapacity()) {
                throw new IllegalArgumentException("Capacity can only be increased, not reduced.");
            }
            event.setEventCapacity(newCapacity);
            eventRepo.save(event);
            eventPublisher.publishCapacityChanged(eventId, newCapacity);
            publishUpdateNotification(eventId, event.getEventName(),
                    "The capacity of this event has been updated to " + newCapacity + " tickets.");
            logger.info("Successfully updated capacity for event ID: " + eventId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to update capacity for event ID: " + eventId + " | Error: " + e.getMessage());
            return false;
        }
    }

    public boolean editEventSeatingMap(String sesionToken, String eventId, SeatingMap seatingMap) {
        if(!authenticationService.validate(extractToken(sesionToken))) {
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
            publishUpdateNotification(eventId, event.getEventName(),
                    "The seating map of this event has been updated.");
            logger.info("Successfully updated seating map for event ID: " + eventId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to update seating map for event ID: " + eventId + " | Error: " + e.getMessage());
            return false;
        }
    }

    public SeatingMap configureSeatingMap(String sessionToken, List<SeatingAreaConfig> seatingAreas,
                                          List<StandingAreaConfig> standingAreas) {
        if(!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        logger.info("Configuring seating map");
        SeatingMap seatingMap = new SeatingMap();
        for (SeatingAreaConfig seatingConfig : seatingAreas) {
            seatingMap.addSeatingArea(seatingConfig.getRows(), seatingConfig.getseatsPerRow(), seatingConfig.getPrice());
        }
        for (StandingAreaConfig standingAreaConfig : standingAreas) {
            seatingMap.addStandingArea(standingAreaConfig.getCapacity(), standingAreaConfig.getPrice());
        }
        logger.info("Seating map configured successfully");
        return seatingMap;
    }

    public boolean addZonesToSeatingMap(String sessionToken, String eventId, List<SeatingAreaConfig> seatingAreas, List<StandingAreaConfig> standingAreas) {
        if (!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        logger.info("Adding zones to seating map for event ID: " + eventId);
        try {
            Event event = eventRepo.findById(eventId);
            if (event == null) {
                logger.warn("Cannot add zones. Event not found: " + eventId);
                return false;
            }
            SeatingMap map = event.getSeatingMap();
            if (map == null) {
                map = new SeatingMap();
                event.setSeatingMap(map);
            }
            for (SeatingAreaConfig seatingConfig : seatingAreas) {
                map.addSeatingArea(seatingConfig.getRows(), seatingConfig.getseatsPerRow(), seatingConfig.getPrice());
            }
            for (StandingAreaConfig standingConfig : standingAreas) {
                map.addStandingArea(standingConfig.getCapacity(), standingConfig.getPrice());
            }
            eventRepo.save(event);
            publishUpdateNotification(eventId, event.getEventName(), "New zones have been added to this event's seating map.");
            logger.info("Successfully added zones to seating map for event ID: " + eventId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to add zones to seating map for event ID: " + eventId + " | Error: " + e.getMessage());
            return false;
        }
    }

    public void releaseSeats(String sessionToken, String orderId, String eventId, List<String> seatIds) {
        if (!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        logger.info("Releasing seats");
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.warn("Cannot release seats. Event not found: " + eventId);
            throw new IllegalArgumentException("Event not found. It may have been removed or the ID is incorrect.");
        }
        if (!event.getSeatingMap().unbookAssignedSeats(seatIds)) {
            logger.warn("Cannot release seats: one or more seats were not booked");
            throw new IllegalArgumentException("Could not release the selected seats. Please refresh and try again.");
        }
        logger.info("Released seats successfully");
    }

    public void releaseStandingArea(String sessionToken, String eventId, String areaID, int quantity){
        if (!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        logger.info("Releasing standing area");
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.warn("Cannot release standing area. Event not found: " + eventId);
            throw new IllegalArgumentException("Event not found. It may have been removed or the ID is incorrect.");
        }
        if(!event.getSeatingMap().unbookStandingArea(areaID, quantity)){
            logger.warn("Cannot release standing area: quantity mismatch or area not booked");
            throw new IllegalArgumentException("Could not release the standing area spots. Please refresh and try again.");
        }
        logger.info("Released standing area successfully");
    }

    public boolean reserveSeats(String sessionToken, String orderId, String eventId, List<String> seatIds){
        if (!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        logger.info("Booking seats");
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.warn("Cannot book seats. Event not found: " + eventId);
            throw new IllegalArgumentException("Event not found. It may have been removed or the ID is incorrect.");
        }
        if (event.getEventDate() != null && event.getEventDate().isBefore(LocalDateTime.now())) {
            logger.warn("Cannot book seats: event has already passed: " + eventId);
            throw new IllegalArgumentException("This event has already taken place and tickets are no longer available.");
        }
        if(!event.getSeatingMap().bookAssignedSeats(seatIds, orderId)){
            logger.warn("Cannot book seats: one or more seats are already taken or do not exist");
            throw new IllegalArgumentException("The selected seats are no longer available. Please choose different seats.");
        }
        logger.info("booked seats successfully");
        return true;
    }

    public boolean reserveStandingArea(String sessionToken, String eventId, String areaId, int quantity){
        if (!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        logger.info("booking standing area");
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.warn("Cannot book standing area. Event not found: " + eventId);
            throw new IllegalArgumentException("Event not found. It may have been removed or the ID is incorrect.");
        }
        if (event.getEventDate() != null && event.getEventDate().isBefore(LocalDateTime.now())) {
            logger.warn("Cannot book standing area: event has already passed: " + eventId);
            throw new IllegalArgumentException("This event has already taken place and tickets are no longer available.");
        }
        if(!event.getSeatingMap().bookStandingArea(areaId, null, quantity)){
            logger.warn("Cannot book standing area: insufficient availability");
            throw new IllegalArgumentException("The standing area does not have enough spots available. Please adjust your quantity.");
        }
        logger.info("booked standing area successfully");
        return true;
    }

    public List<String> checkSeatsReserved(String sessionToken, String orderId, String eventId, List<String> seatIds){
        if (!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        logger.info("checking reserved seats");
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.warn("Cannot check reserved seats. Event not found: " + eventId);
            throw new IllegalArgumentException("Event not found. It may have been removed or the ID is incorrect.");
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

    public boolean editEventLocation(String sessionToken, String eventId, String newLocation) {
        if (!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        try {
            Event event = eventRepo.findById(eventId);
            if (event == null) return false;
            event.setLocation(newLocation);
            eventRepo.save(event);
            String locationDesc = newLocation != null ? "to \"" + newLocation + "\"" : "to unspecified";
            publishUpdateNotification(eventId, event.getEventName(),
                    "The location of this event has been updated " + locationDesc + ".");
            return true;
        } catch (Exception e) {
            logger.error("Failed to edit event location for ID: " + eventId + " | Error: " + e.getMessage());
            return false;
        }
    }

    public boolean editEventPrice(String sessionToken, String eventId, Double newPrice) {
        if (!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        try {
            Event event = eventRepo.findById(eventId);
            if (event == null) return false;
            eventRepo.save(event);
            publishUpdateNotification(eventId, event.getEventName(),
                    "The ticket price of this event has been updated.");
            return true;
        } catch (Exception e) {
            logger.error("Failed to edit event price for ID: " + eventId + " | Error: " + e.getMessage());
            return false;
        }
    }

    public boolean editEventPurchasePolicy(String sesssionToken, String eventId, PurchasePolicyDTO purchasePolicyDTO){
        if(!authenticationService.validate(extractToken(sesssionToken))){
            throw new IllegalArgumentException("Invalid session token");
        }
        if (!purchasePolicyDTO.isQuantityOr() && purchasePolicyDTO.minTickets() != null && purchasePolicyDTO.maxTickets() != null
                && purchasePolicyDTO.minTickets() > purchasePolicyDTO.maxTickets()) {
            throw new IllegalArgumentException("minTickets cannot be greater than maxTickets in AND rule");
        }
        if (!purchasePolicyDTO.isAgeOr() && purchasePolicyDTO.minAge() != null && purchasePolicyDTO.maxAge() != null
                && purchasePolicyDTO.minAge() > purchasePolicyDTO.maxAge()) {
            throw new IllegalArgumentException("minAge cannot be greater than maxAge in AND rule");
        }
        Event event = eventRepo.findById(eventId);
        if(event == null){
            logger.warn("Cannot check reserved seats. Event not found: " + eventId);
            throw new RuntimeException("couldnt find event by eventId "+ eventId);
        }
        event.setPurchasePolicy(purchasePolicyDTO);
        try {
            eventRepo.save(event);
            publishUpdateNotification(eventId, event.getEventName(),
                    buildPolicyDescription(purchasePolicyDTO));
            logger.info("changed purchase policy of event with id: " + eventId);
            return true;
        }catch (Exception e){
            logger.error("Failed to edit event purchase policy for ID: " + eventId + " | Error: " + e.getMessage());
            return false;
        }
    }

    private void publishUpdateNotification(String eventId, String eventName, String description) {
        try {
            eventPublisher.publishEventUpdated(eventId, eventName, description);
        } catch (Exception e) {
            logger.warn("Failed to send update notification for event " + eventId + ": " + e.getMessage());
        }
    }

    private String buildPolicyDescription(PurchasePolicyDTO dto) {
        List<String> parts = new ArrayList<>();

        // Ticket quantity constraints
        if (dto.minTickets() != null && dto.maxTickets() != null) {
            String connector = dto.isQuantityOr() ? " or " : " and ";
            parts.add("you can buy between " + dto.minTickets() + connector + dto.maxTickets() + " tickets");
        } else if (dto.minTickets() != null) {
            parts.add("you must buy at least " + dto.minTickets() + " ticket(s)");
        } else if (dto.maxTickets() != null) {
            parts.add("you can buy up to " + dto.maxTickets() + " ticket(s)");
        }

        // Age constraints
        if (dto.minAge() != null && dto.maxAge() != null) {
            String connector = dto.isAgeOr() ? " or " : " - ";
            parts.add("the allowed age range is " + dto.minAge() + connector + dto.maxAge());
        } else if (dto.minAge() != null) {
            parts.add("you must be at least " + dto.minAge() + " years old");
        } else if (dto.maxAge() != null) {
            parts.add("you must be at most " + dto.maxAge() + " years old");
        }

        if (parts.isEmpty()) {
            return "The purchase policy of this event has been updated (no restrictions).";
        }

        String combined = String.join(", and ", parts);
        return "The purchase policy of this event has been updated: " + combined + ".";
    }

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

    public boolean checkStandingAreaAvailability(String eventId, String areaId, int quantity) {
        if (quantity <= 0) return false;
        Event event = eventRepo.findById(eventId);
        if (event == null || event.getSeatingMap() == null) return false;
        StandingArea area = event.getSeatingMap().getArea(areaId);
        return area != null && area.getAvalibleSeatNumber() >= quantity;
    }

    public PurchasePolicyDTO getEventPurchasePolicy(String sessionToken, String eventId) {
        if (!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.warn("Cannot get purchase policy. Event not found: " + eventId);
            throw new IllegalArgumentException("Event not found. It may have been removed or the ID is incorrect.");
        }
        return event.getPurchasePolicy().getDTO();
    }

    public SeatingMapDTO getEventSeatingMap(String sessionToken, String eventId) {
        if (!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.warn("Cannot get seating map. Event not found: " + eventId);
            throw new IllegalArgumentException("Event not found. It may have been removed or the ID is incorrect.");
        }
        if (event.getSeatingMap() == null) {
            throw new IllegalStateException("Event has no seating map configured");
        }
        return event.getSeatingMap().getDTO();
    }

    public String validatePurchasePolicy(String sessionToken, String eventId, int quantity, int userAge) {
        if (!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found. It may have been removed or the ID is incorrect.");
        }
        PurchaseContext context = new PurchaseContext(quantity, userAge);
        if (event.getPurchasePolicy().validate(context)) {
            return null;
        }
        return buildPolicyViolationMessage(event.getPurchasePolicy().getDTO(), quantity, userAge);
    }

    private String buildPolicyViolationMessage(PurchasePolicyDTO dto, int quantity, int userAge) {
        List<String> violations = new ArrayList<>();
        if (dto.minTickets() != null && quantity < dto.minTickets())
            violations.add("minimum " + dto.minTickets() + " ticket(s) required");
        if (dto.maxTickets() != null && quantity > dto.maxTickets())
            violations.add("maximum " + dto.maxTickets() + " ticket(s) allowed");
        if (dto.minAge() != null && userAge < dto.minAge())
            violations.add("minimum age " + dto.minAge() + " required");
        if (dto.maxAge() != null && userAge > dto.maxAge())
            violations.add("maximum age " + dto.maxAge() + " required");
        if (violations.isEmpty())
            return "Purchase policy requirements not met.";
        return "Purchase policy violated: " + String.join(", ", violations) + ".";
    }

    public List<EventDTO> searchActiveEventsByText(String query) {
        //getting all active events from the repo
        List<Event> activeEvents = eventRepo.findActiveEvents();

        //getting event that contains the query in their name 
        activeEvents = activeEvents.stream()
                .filter(event -> event.getEventName().toLowerCase().contains(query.toLowerCase()))
                .toList();
        //mapping to DTOs
        List<EventDTO> events = activeEvents.stream().map(this::toDTO).toList();

        logger.info("Found " + events.size() + " active events matching query: " + query);
        return events;
    }
    public int getEventCompanyId(String token, String eventId){
        if (!authenticationService.validate(extractToken(token))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found. It may have been removed or the ID is incorrect.");
        }
        return event.getCompanyId();
    }
}