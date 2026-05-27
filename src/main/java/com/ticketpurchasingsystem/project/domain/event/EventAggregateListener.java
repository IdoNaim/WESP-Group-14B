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
    // You can inject repositories or handlers here if needed, just like in UserListener
    public EventAggregateListener(IEventRepo eventRepo, IEventService eventService) {
        this.eventRepo = eventRepo;
        this.eventService = eventService;
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
        PurchaseContext context = new PurchaseContext(  
                event.getTotalTickets(),
                event.getAge()        );

        boolean isValid = policy.validate(context);
        event.setResult(isValid);
    }
    @EventListener
    public void onIsValidEventIDEvent(IsValidEventIDEvent event){
        event.setResult(eventRepo.findById(event.getEventId()) != null);
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