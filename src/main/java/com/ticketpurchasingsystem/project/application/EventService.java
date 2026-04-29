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

    public boolean createEvent(EventDTO eventDTO,
                               PurchasePolicyDTO purchasePolicyDTO,
                               List<DiscountDTO> discountPolicyDTO) {

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

        try {
            eventRepo.save(event);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public EventDTO searchEvent(String eventId) {
        Event event = eventRepo.findById(eventId);

        if (event == null) {
            return null;
        }

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

        return eventRepo.findByCompanyId(companyId)
                .stream()
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
        try {
            Event event = eventRepo.findById(eventId);

            if (event == null) {
                return false;
            }

            event.setEventDate(newDateTime);
            eventRepo.save(event);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean removeEvent(String eventId) {
        try {
            Event event = eventRepo.findById(eventId);

            if (event == null) {
                return false;
            }

            eventRepo.delete(eventId);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean editEventInventory(String eventId, int newCapacity) {
        throw new UnsupportedOperationException("Unimplemented method 'editEventInventory'");
    }

    @Override
    public boolean configureEventSeatinMap(String eventId, SeatingMap seatingMapDTO) {
        throw new UnsupportedOperationException("Unimplemented method 'configureEventSeatinMap'");
    }
}