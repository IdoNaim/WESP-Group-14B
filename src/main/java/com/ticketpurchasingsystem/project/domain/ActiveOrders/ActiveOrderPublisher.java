package com.ticketpurchasingsystem.project.domain.ActiveOrders;
import org.springframework.context.ApplicationEventPublisher;

import com.ticketpurchasingsystem.project.application.IPaymentGateway;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.*;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.SeatReservationEvent;
public class ActiveOrderPublisher {
    private ApplicationEventPublisher eventPublisher;

    public ActiveOrderPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public boolean publishIsValidEventIDEvent(String eventId) {
        IsValidEventIDEvent event = new IsValidEventIDEvent(this, eventId);
        eventPublisher.publishEvent(event);
        return event.isValid();
    }


    public boolean publishReserveSeats(String eventId, String[] seatIds) {
        SeatReservationEvent event = new SeatReservationEvent(this, eventId, seatIds);
        eventPublisher.publishEvent(event);
        return event.getResult();
    }

    public boolean publishReserveStandingArea(String eventId, String areaId, int quantity) {
        StandingAreaReservationEvent event = new StandingAreaReservationEvent(this, eventId, areaId, quantity);
        eventPublisher.publishEvent(event);
        return event.getResult();
    }


    // public boolean publishReserveTickets(String eventId, int quantity)
    // {
    //     TicketReservationEvent event = new TicketReservationEvent(this,eventId, quantity);
    //     eventPublisher.publishEvent(event);
    //     return event.getResult();
    // }
    public boolean publishIsMember(String userId){
        IsMemberEvent event = new IsMemberEvent(this, userId);
        eventPublisher.publishEvent(event);
        return event.getResult();
    }
    public void publishUnreserveTickets(String eventID, int quantity){
        TicketUnreservationEvent event = new TicketUnreservationEvent(this, eventID, quantity);
        eventPublisher.publishEvent(event);
    }

    // public boolean publishPaymentEvent(IPaymentGateway paymentGateway, SessionToken sessionToken, double amount) {
    //     // TODO Auto-generated method stub
    //     throw new UnsupportedOperationException("Unimplemented method 'publishPaymentEvent'");
    // }
}
