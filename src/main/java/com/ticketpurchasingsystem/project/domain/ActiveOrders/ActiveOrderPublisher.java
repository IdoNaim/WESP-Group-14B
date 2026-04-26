package com.ticketpurchasingsystem.project.domain.ActiveOrders;
import org.springframework.context.ApplicationEventPublisher;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.*;
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



    public boolean publishReserveTickets(String eventId, int quantity)
    {
        TicketReservationEvent event = new TicketReservationEvent(this,eventId, quantity);
        eventPublisher.publishEvent(event);
        return event.getResult();
    }
    public boolean publishIsMember(String userId){
        IsMemberEvent event = new IsMemberEvent(this, userId);
        eventPublisher.publishEvent(event);
        return event.getResult();
    }
    public void publishUnreserveTickets(String eventID, int quantity){
        TicketUnreservationEvent event = new TicketUnreservationEvent(this, eventID, quantity);
        eventPublisher.publishEvent(event);
    }
}
