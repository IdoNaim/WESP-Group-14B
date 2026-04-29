package com.ticketpurchasingsystem.project.application;

import java.time.LocalDateTime;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.event.Event;
import com.ticketpurchasingsystem.project.domain.event.EventDiscountPolicy;
import com.ticketpurchasingsystem.project.domain.event.EventListener;
import com.ticketpurchasingsystem.project.domain.event.EventPublisher;
import com.ticketpurchasingsystem.project.domain.event.EventPurchasePolicy;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;
import com.ticketpurchasingsystem.project.domain.event.SeatingMap;

public class EventService implements IEventService {
    private final IEventRepo eventRepo;
    EventPublisher eventPublisher = EventPublisher.getInstance();
    EventListener eventListener = EventListener.getInstance();
        public EventService(IEventRepo eventRepo) {
        this.eventRepo = eventRepo;
    }

    public boolean createEvent(EventDTO eventDTO, PurchasePolicyDTO purchasePolicyDTO, List<DiscountDTO> discountPolicyDTO) {
        // Convert DTOs to domain objects
        EventPurchasePolicy purchasePolicy = new EventPurchasePolicy(
            purchasePolicyDTO.minTickets(),
            purchasePolicyDTO.maxTickets(),
            purchasePolicyDTO.minAge(),
            purchasePolicyDTO.maxAge(),
            purchasePolicyDTO.emnptySeatLeft()
        );
        EventDiscountPolicy discountPolicy = new EventDiscountPolicy(discountPolicyDTO);
        Event event = new Event(
                eventDTO.companyId(),
                eventDTO.eventName(),
                eventDTO.eventCapacity(),
                eventDTO.eventDateTime(),
                purchasePolicy,
                discountPolicy
        );
        try{
            eventRepo.save(event);
            //eventPublisher.publishEventCreated(event);
            return true;
        } catch (Exception e) {
            // Handle exceptions (e.g., log the error)
            return false;
        }
    }
    public EventDTO searchEvent(String eventId) {
        Event event = eventRepo.findById(eventId).orElse(null);
        if (event == null) {
            return null;
        }
        // Convert domain object to DTO
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
        List<Event> events = eventRepo.findByCompanyId(companyId);
        // Convert domain objects to DTOs
        return events.stream()
                .map(event -> new EventDTO(
                        event.getCompanyId(),
                        event.getEventName(),
                        event.getEventCapacity(),
                        event.getEventDate(),
                        event.isActive()
                ))
                .toList();
    }
    @Override
    public boolean editEventDate(String eventId, LocalDateTime newDateTime) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'editEventDate'");
    }
}

    @Override
    public boolean removeEvent(String eventId) {
        return eventRepo.findById(eventId)
                .map(event -> {
                    eventRepo.delete(eventId);
                    //eventPublisher.publishEventRemoved(event);
                    return true;
                })
                .orElse(false);
    } //rr

    @Override
    public boolean editEventInventory(String eventId, int newCapacity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'editEventInventory'");
    }
    @Override
    public boolean configureEventSeatinMap(String eventId, SeatingMap seatingMapDTO) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'configureEventSeatinMap'");
    }
    

    
}
