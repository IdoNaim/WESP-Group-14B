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

        // Hooks into the EventHandler Singleton using the required components
        this.eventHandler = EventHandler.getInstance(eventRepo, eventPublisher, authenticationService);

        logger.info("EventService initialized using EventHandler singleton instance");
    }

    @Override
    public boolean createEvent(String sessionToken, EventDTO eventDTO,
                               PurchasePolicyDTO purchasePolicyDTO,
                               List<DiscountDTO> discountPolicyDTO) {
        return eventHandler.createEvent(sessionToken, eventDTO, purchasePolicyDTO, discountPolicyDTO);
    }

    @Override
    public List<EventDTO> getAllActiveEvents() {
        return eventHandler.getAllActiveEvents();
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
}