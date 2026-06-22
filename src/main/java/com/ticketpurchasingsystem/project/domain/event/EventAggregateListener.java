package com.ticketpurchasingsystem.project.domain.event;

import com.ticketpurchasingsystem.project.application.IEventService;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.GetCompanyIdEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.IsValidEventIDEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.SeatReleaseEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.SeatReservationEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.StandingAreaReleaseEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.StandingAreaReservationEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.IsUpToPolicyEvent;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.*;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;

import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

// Import your custom events
import com.ticketpurchasingsystem.project.domain.event.Events_Events.EventCreatedEvent;
import com.ticketpurchasingsystem.project.domain.event.Events_Events.EventCapacityChangedEvent;

import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.EventPurchasePolicy;

import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.PurchaseContext;

@Component
public class EventAggregateListener {
    IEventRepo eventRepo;
    IEventService eventService;
    IHistoryOrderRepo historyOrderRepo;

    public EventAggregateListener(IEventRepo eventRepo, IEventService eventService, IHistoryOrderRepo historyOrderRepo) {
        this.eventRepo = eventRepo;
        this.eventService = eventService;
        this.historyOrderRepo = historyOrderRepo;
    }

    @EventListener
    public void onEventCreated(EventCreatedEvent event) {
        loggerDef.getInstance().info(
                "New Event created: ID=" + event.getEvent().getEventId() +
                        ", Name=" + event.getEvent().getEventName()
        );
        // Handle logic, notify other aggregates, update read models, etc.
    }

    @EventListener
    public void onCapacityChanged(EventCapacityChangedEvent event) {
        loggerDef.getInstance().info(
                "Event Capacity Updated: ID=" + event.getEventId() +
                        ", New Capacity=" + event.getNewCapacity()
        );
        // Handle logic
    }
    @EventListener
    public void onGetCompanyIdEvent(GetCompanyIdEvent event){
        Event e = eventRepo.findById(event.getEventId());
        if(e == null){
            event.setResult(null);
            return;
        }
        event.setResult(e.getCompanyId());
    }

    @EventListener
    public void onIsUpToPolicyEvent(IsUpToPolicyEvent event){
        Event e = eventRepo.findById(event.getEventID());
        if(e == null){
            event.setResult(false);
            return;
        }
        EventPurchasePolicy policy = e.getPurchasePolicy();
        if(policy == null){
            event.setResult(true); // No policy means no restrictions
            return;
        }

        int alreadyPurchased = 0;
        String userId = event.userID();
        if (userId != null && !userId.isEmpty()) {
            List<HistoryOrderItem> pastOrders = historyOrderRepo.findAllByUserIdAndEventId(userId, event.getEventID());
            for (HistoryOrderItem past : pastOrders) {
                alreadyPurchased += past.getSeatIds().size();
                for (int qty : past.getStandingAreaQuantities().values()) {
                    alreadyPurchased += qty;
                }
            }
        }

        PurchaseContext context = new PurchaseContext(
                event.getTotalTickets(),
                event.getAge(),
                alreadyPurchased);

        boolean isValid = policy.validate(context);
        event.setResult(isValid);
    }
    @EventListener
    public void onIsValidEventIDEvent(IsValidEventIDEvent event){
        Event e = eventRepo.findById(event.getEventId());
        event.setResult(e != null && e.isActive());
    }
    @EventListener
    public void onSeatReleaseEvent(SeatReleaseEvent event){
        eventService.releaseSeats(event.getSessionToken(), event.getOrderID(), event.getEventID(), event.getSeatIds());
    }
    @EventListener
    public void onStandingAreaReleaseEvent(StandingAreaReleaseEvent event){
        eventService.releaseStandingArea(event.getSessionToken(), event.getEventID(), event.getAreaID(), event.getQuantity());
    }
    @EventListener
    public void onSeatReservationEvent(SeatReservationEvent event){
        try{
            boolean success = eventService.reserveSeats(event.getSessionToken(), event.getOrderID(), event.getEventID(), event.getSeatIds());
            if(success){
                event.setResult(true);
            }
            else{
                event.setResult(false);
            }
        }catch(Exception e){
            event.setResult(false);
        }
    }
    @EventListener
    public void onStandingAreaReservationEvent(StandingAreaReservationEvent event){
        try{
            boolean success = eventService.reserveStandingArea(event.getSessionToken(), event.getEventId(), event.getAreaId(), event.getQuantity());
            if(success){
                event.setResult(true);
            }
            else{
                event.setResult(false);
            }
        }catch(Exception e){
            event.setResult(false);
        }
    }
    @EventListener
    public void onCheckSeatsReservedEvent(CheckSeatsReservedEvent event){
        try{
            List<String> reservedSeats = eventService.checkSeatsReserved(event.getSessionToken(), event.getOrderId(), event.getEventId(), event.getSeatIds());
            event.setResult(reservedSeats);
        }catch(Exception e){
            event.setResult(List.of());
        }
    }
}