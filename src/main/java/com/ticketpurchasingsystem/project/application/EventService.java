package com.ticketpurchasingsystem.project.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.event.EventAggregatePublisher;
import com.ticketpurchasingsystem.project.domain.event.EventHandler;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;
import org.springframework.stereotype.Service;
import com.ticketpurchasingsystem.project.domain.Utils.SeatingMapDTO;
import com.ticketpurchasingsystem.project.domain.event.Event;
import com.ticketpurchasingsystem.project.domain.event.EventAggregatePublisher;
import com.ticketpurchasingsystem.project.domain.event.EventDiscountPolicy;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;
import com.ticketpurchasingsystem.project.domain.event.Maps.AssignedSeat;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.AndRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.OrRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.PurchaseContext;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.EventPurchasePolicy;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.MaxAgeRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.MaxTicketsRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.MinAgeRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.MinTicketsRule;
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

    @Override
    public PurchasePolicyDTO getEventPurchasePolicy(String sessionToken, String eventId) {
        if (!authenticationService.validate(sessionToken)) {
            throw new IllegalArgumentException("Invalid session token");
        }
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            logger.warn("Cannot get purchase policy. Event not found: " + eventId);
            throw new IllegalArgumentException("Invalid EventID");
        }
        return event.getPurchasePolicy().getDTO();
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