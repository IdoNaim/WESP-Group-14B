package com.ticketpurchasingsystem.project.application;

import java.time.LocalDateTime;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.event.EventAggregatePublisher;
import com.ticketpurchasingsystem.project.domain.event.EventHandler;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;
import org.springframework.stereotype.Service;

import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;

import com.ticketpurchasingsystem.project.domain.Utils.SeatingMapDTO;
import com.ticketpurchasingsystem.project.domain.event.*;
import com.ticketpurchasingsystem.project.domain.event.Maps.AssignedSeat;

import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingAreaConfig;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingMap;
import com.ticketpurchasingsystem.project.domain.event.Maps.StandingAreaConfig;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

@Service
public class EventService implements IEventService {

    private final EventHandler eventHandler;
    private final loggerDef logger = loggerDef.getInstance();

    // Constructor parameters match your original implementation perfectly
    public EventService(IEventRepo eventRepo,
                        EventAggregatePublisher eventPublisher,
                        AuthenticationService authenticationService) {

//<<<<<<< HEAD
//        // EventPurchasePolicy defaultPurchasePolicy = new EventPurchasePolicy();
//        // defaultPurchasePolicy.addRule(new MinAgeRule(18));
//        // Event testEvent = new Event(
//        //         1,
//        //         "Test Event",
//        //         100,
//        //         LocalDateTime.now().plusDays(10),
//        //         defaultPurchasePolicy,
//        //         new EventDiscountPolicy(new ArrayList<>()),
//        //         0
//        // );
//        // testEvent.setEventLocation("Test Location");
//        // SeatingMap seatingMap = new SeatingMap();
//        // seatingMap.addSeatingArea(10, 10, 40.0);
//        // seatingMap.addStandingArea(50, 20.0);
//        // seatingMap.bookAssignedSeats(List.of("0_1_1"), "1");
//        // seatingMap.bookStandingArea("1", "1", 2);
//        // testEvent.setSeatingMap(seatingMap);
//        // Event eventWithId =eventRepo.save(testEvent);
//        // logger.info("Created test event with ID: " + eventWithId.getEventId());
//        logger.info("EventService initialized");
//    }
//
//    private String extractToken(String token) {
//        if (token != null && token.startsWith("Bearer ")) {
//            return token.substring(7);
//        }
//        return token;
//=======
        // Hooks into the EventHandler Singleton using the required components
        this.eventHandler = EventHandler.getInstance(eventRepo, eventPublisher, authenticationService);

        logger.info("EventService initialized using EventHandler singleton instance");
//>>>>>>> 977e62e60538a55f7f25f0ed01751af487fbb0b6
    }

    @Override
    public boolean createEvent(String sessionToken, EventDTO eventDTO,
                               PurchasePolicyDTO purchasePolicyDTO,
                               List<DiscountDTO> discountPolicyDTO) {
        return eventHandler.createEvent(sessionToken, eventDTO, purchasePolicyDTO, discountPolicyDTO);
    }

    @Override
    public EventDTO searchEvent(String sessionToken, String eventId) {
        return eventHandler.searchEvent(sessionToken, eventId);
    }

    @Override
    public List<EventDTO> searchEventsByCompany(String sessionToke, int companyId) {
        return eventHandler.searchEventsByCompany(sessionToke, companyId);
    }

    @Override
    public boolean editEventDate(String sessionToken, String eventId, LocalDateTime newDateTime) {
        return eventHandler.editEventDate(sessionToken, eventId, newDateTime);
    }

    @Override
    public boolean removeEvent(String sessionToken, String eventId) {
        return eventHandler.removeEvent(sessionToken, eventId);
    }

    @Override
    public boolean editEventInventory(String sessionToken, String eventId, int newCapacity) {
        return eventHandler.editEventInventory(sessionToken, eventId, newCapacity);
    }

    @Override
    public boolean editEventSeatingMap(String sesionToken, String eventId, SeatingMap seatingMap) {
        return eventHandler.editEventSeatingMap(sesionToken, eventId, seatingMap);
    }

    @Override
    public SeatingMap configureSeatingMap(String sessionToken, List<SeatingAreaConfig> seatingAreas,
                                          List<StandingAreaConfig> standingAreas) {
        return eventHandler.configureSeatingMap(sessionToken, seatingAreas, standingAreas);
    }

    @Override
    public void releaseSeats(String sessionToken, String orderId, String eventId, List<String> seatIds) {
        eventHandler.releaseSeats(sessionToken, orderId, eventId, seatIds);
    }
//<<<<<<< HEAD
//    public void releaseStandingArea(String sessionToken, String eventId, String areaID, int quantity){
//        //TODO: Implement the logic to release reserved standing area based on the eventId, areaId, and quantity
//        if (!authenticationService.validate(extractToken(sessionToken))) {
//            throw new IllegalArgumentException("Invalid session token");
//        }
//        logger.info("Releasing standing area");
//        Event event = eventRepo.findById(eventId);
//        if (event == null) {
//            logger.warn("Cannot release standing area. Event not found: " + eventId);
//            throw new IllegalArgumentException("Invalid EventID");
//        }
//        if(!event.getSeatingMap().unbookStandingArea(areaID, quantity)){
//            logger.warn("Cannot release standing area. one or more stands not booked");
//            // throw new IllegalArgumentException("one or more stands not booked");
//        }
//        else{
//        logger.info("Released standing area successfully");
//        }
//    }
//    public boolean reserveSeats(String sessionToken, String orderId, String eventId, List<String> seatIds){
//        //TODO: Implement the logic to reserve seats based on the orderId, eventId, and seatIds
//        if (!authenticationService.validate(extractToken(sessionToken))) {
//            throw new IllegalArgumentException("Invalid session token");
//        }
//        logger.info("Releasing seats");
//        Event event = eventRepo.findById(eventId);
//        if (event == null) {
//            logger.warn("Cannot release seats. Event not found: " + eventId);
//            throw new IllegalArgumentException("Invalid EventID");
//        }
//        if(!event.getSeatingMap().bookAssignedSeats(seatIds, orderId)){
//            logger.warn("Cannot book seats, problem occured");
//            throw new IllegalArgumentException("cannot book seats, problem occured");
//        }
//        logger.info("booked seats successfully");
//        return true;
//    }
//    public boolean reserveStandingArea(String sessionToken, String eventId, String areaId, int quantity){
//        //TODO: Implement the logic to reserve standing area based on the eventId, areaId, and quantity
//        if (!authenticationService.validate(extractToken(sessionToken))) {
//            throw new IllegalArgumentException("Invalid session token");
//        }
//        logger.info("booking standing area");
//        Event event = eventRepo.findById(eventId);
//        if (event == null) {
//            logger.warn("Cannot book standing area. Event not found: " + eventId);
//            throw new IllegalArgumentException("Invalid EventID");
//        }
//        if(!event.getSeatingMap().bookStandingArea(areaId, null,quantity)){
//            logger.warn("Cannot book standing area. one or more stands not booked");
//            throw new IllegalArgumentException("cannot book standing area, problem occured");
//        }
//        logger.info("booked standing area successfully");
//        return true;
//=======
//>>>>>>> 977e62e60538a55f7f25f0ed01751af487fbb0b6

    @Override
    public void releaseStandingArea(String sessionToken, String eventId, String areaID, int quantity) {
        eventHandler.releaseStandingArea(sessionToken, eventId, areaID, quantity);
    }

    @Override
    public boolean reserveSeats(String sessionToken, String orderId, String eventId, List<String> seatIds) {
        return eventHandler.reserveSeats(sessionToken, orderId, eventId, seatIds);
    }

    @Override
    public boolean reserveStandingArea(String sessionToken, String eventId, String areaId, int quantity) {
        return eventHandler.reserveStandingArea(sessionToken, eventId, areaId, quantity);
    }

    @Override
    public List<String> checkSeatsReserved(String sessionToken, String orderId, String eventId, List<String> seatIds) {
        return eventHandler.checkSeatsReserved(sessionToken, orderId, eventId, seatIds);
    }

    @Override
    public boolean editEventLocation(String sessionToken, String eventId, String newLocation) {
        return eventHandler.editEventLocation(sessionToken, eventId, newLocation);
    }

    @Override
    public boolean editEventPrice(String sessionToken, String eventId, Double newPrice) {
        return eventHandler.editEventPrice(sessionToken, eventId, newPrice);
    }

    @Override
    public boolean editEventPurchasePolicy(String sesssionToken, String eventId, PurchasePolicyDTO purchasePolicyDTO) {
        return eventHandler.editEventPurchasePolicy(sesssionToken, eventId, purchasePolicyDTO);
    }

    @Override
    public boolean checkSeatAvailability(String eventId, List<String> seatIds) {
        return eventHandler.checkSeatAvailability(eventId, seatIds);
    }

    @Override
    public boolean checkStandingAreaAvailability(String eventId, String areaId, int quantity) {
        return eventHandler.checkStandingAreaAvailability(eventId, areaId, quantity);
    }
//<<<<<<< HEAD
    @Override
    public SeatingMapDTO getEventSeatingMap(String sessionToken, String eventId) {
        return eventHandler.getEventSeatingMap( sessionToken,  eventId);

    }

    @Override
    public String validatePurchasePolicy(String sessionToken, String eventId, int quantity, int userAge) {
        return eventHandler.validatePurchasePolicy(sessionToken, eventId, quantity, userAge);
    }

}
//=======
//}
//>>>>>>> 977e62e60538a55f7f25f0ed01751af487fbb0b6
