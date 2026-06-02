package com.ticketpurchasingsystem.project.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

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

        EventPurchasePolicy defaultPurchasePolicy = new EventPurchasePolicy();
        defaultPurchasePolicy.addRule(new MinAgeRule(18));
        Event testEvent = new Event(
                1,
                "Test Event",
                100,
                LocalDateTime.now().plusDays(10),
                defaultPurchasePolicy,
                new EventDiscountPolicy(new ArrayList<>()),
                0
        );
        testEvent.setEventLocation("Test Location");
        SeatingMap seatingMap = new SeatingMap();
        seatingMap.addSeatingArea(10, 10, 40.0); 
        seatingMap.addStandingArea(50, 20.0);
        seatingMap.bookAssignedSeats(List.of("0_1_1"), "1");
        seatingMap.bookStandingArea("1", "1", 2);
        testEvent.setSeatingMap(seatingMap);
        Event eventWithId =eventRepo.save(testEvent);
        logger.info("Created test event with ID: " + eventWithId.getEventId());
        logger.info("EventService initialized");
    }

    private String extractToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    public boolean createEvent(String sessionToken, EventDTO eventDTO,
                               PurchasePolicyDTO purchasePolicyDTO,
                               List<DiscountDTO> discountPolicyDTO) {
        if(!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        logger.info("Creating event: " + eventDTO.eventName());

        // --- VALIDATION LAYER ---
        if (eventDTO.eventDateTime() != null && eventDTO.eventDateTime().isBefore(LocalDateTime.now())) {
            logger.error("Failed to create event: event date cannot be in the past");
            return false;
        }

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
        event.setEventLocation(eventDTO.eventLocation());
        event.setTicketPrice(eventDTO.ticketPrice());

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

        return new EventDTO(
                event.getEventId(),
                event.getCompanyId(),
                event.getEventName(),
                event.getEventCapacity(),
                event.getEventDate(),
                event.isActive(),
                event.getEventLocation(),
                event.getTicketPrice()
        );
    }

    @Override
    public List<EventDTO> searchEventsByCompany(String sessionToke, int companyId) {
        if(!authenticationService.validate(extractToken(sessionToke))) {
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
                        event.isActive(),
                        event.getEventLocation(),
                        event.getTicketPrice()
                ))
                .toList();

        logger.info("Found " + events.size() + " events for company ID: " + companyId);

        return events;
    }

    @Override
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
        if(!authenticationService.validate(extractToken(sessionToken))) {
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
        if(!authenticationService.validate(extractToken(sessionToken))) {
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
        if (!authenticationService.validate(extractToken(sessionToken))) {
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
        if (!authenticationService.validate(extractToken(sessionToken))) {
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
            // throw new IllegalArgumentException("one or more stands not booked");
        }
        else{
        logger.info("Released standing area successfully");
        }
    }
    public boolean reserveSeats(String sessionToken, String orderId, String eventId, List<String> seatIds){
        //TODO: Implement the logic to reserve seats based on the orderId, eventId, and seatIds
        if (!authenticationService.validate(extractToken(sessionToken))) {
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
        if (!authenticationService.validate(extractToken(sessionToken))) {
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
        if (!authenticationService.validate(extractToken(sessionToken))) {
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
    public boolean editEventLocation(String sessionToken, String eventId, String newLocation) {
        if (!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        try {
            Event event = eventRepo.findById(eventId);
            if (event == null) return false;
            event.setEventLocation(newLocation);
            eventRepo.save(event);
            return true;
        } catch (Exception e) {
            logger.error("Failed to edit event location for ID: " + eventId + " | Error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean editEventPrice(String sessionToken, String eventId, Double newPrice) {
        if (!authenticationService.validate(extractToken(sessionToken))) {
            throw new IllegalArgumentException("Invalid session token");
        }
        try {
            Event event = eventRepo.findById(eventId);
            if (event == null) return false;
            event.setTicketPrice(newPrice);
            eventRepo.save(event);
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
        if (purchasePolicyDTO.minTickets() != null && purchasePolicyDTO.maxTickets() != null
                && purchasePolicyDTO.minTickets() > purchasePolicyDTO.maxTickets()) {
            throw new IllegalArgumentException("minTickets cannot be greater than maxTickets");
        }
        if (purchasePolicyDTO.minAge() != null && purchasePolicyDTO.maxAge() != null
                && purchasePolicyDTO.minAge() > purchasePolicyDTO.maxAge()) {
            throw new IllegalArgumentException("minAge cannot be greater than maxAge");
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
    @Override
    public SeatingMapDTO getEventSeatingMap(String sessionToken, String eventId) {
        if (!authenticationService.validate(sessionToken)) {
            throw new IllegalArgumentException("Invalid session token");
        }
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.warn("Cannot get seating map. Event not found: " + eventId);
            throw new IllegalArgumentException("Invalid EventID");
        }
        return event.getSeatingMap().getDTO();
    }

    @Override
    public String validatePurchasePolicy(String sessionToken, String eventId, int quantity, int userAge) {
        if (!authenticationService.validate(sessionToken)) {
            throw new IllegalArgumentException("Invalid session token");
        }
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Invalid EventID");
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
}
