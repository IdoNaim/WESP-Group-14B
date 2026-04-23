package com.ticketpurchasingsystem.project.application;

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
import com.ticketpurchasingsystem.project.infrastructure.EventRepo;

public class EventService {
    IEventRepo eventRepo = EventRepo.getInstance();
    EventPublisher eventPublisher = EventPublisher.getInstance();
    EventListener eventListener = EventListener.getInstance();
    private static EventService instance;
    public static EventService getInstance() {
        if (instance == null) {
            instance = new EventService();
        }
        return instance;
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
    

    
}
