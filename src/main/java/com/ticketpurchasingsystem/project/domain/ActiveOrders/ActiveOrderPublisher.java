package com.ticketpurchasingsystem.project.domain.ActiveOrders;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.*;

@Component
public class ActiveOrderPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public ActiveOrderPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public boolean publishIsValidEventIDEvent(String eventId) {
        IsValidEventIDEvent event = new IsValidEventIDEvent(this, eventId);
        eventPublisher.publishEvent(event);
        return event.isValid();
    }


    public boolean publishReserveSeats(String sessionToken, String orderId, String eventId, List<String> seatIds) {
        SeatReservationEvent event = new SeatReservationEvent(this, sessionToken, eventId, seatIds, orderId);
        eventPublisher.publishEvent(event);
        return event.getResult();
    }
    public void publishReleaseSeats(String sessionToken, String orderId, String eventId, List<String> seatIds){
        SeatReleaseEvent event = new SeatReleaseEvent(this, sessionToken, eventId, seatIds, orderId);
        eventPublisher.publishEvent(event);
    }

    public boolean publishReserveStandingArea(String sessionToken, String eventId, String areaId, int quantity) {
        StandingAreaReservationEvent event = new StandingAreaReservationEvent(this, sessionToken, eventId, areaId, quantity);
        eventPublisher.publishEvent(event);
        return event.getResult();
    }
    public void publishReleaseStandingArea(String sessionToken, String eventId, String areaID, int quantity){
        StandingAreaReleaseEvent event = new StandingAreaReleaseEvent(this, sessionToken, eventId, areaID, quantity);
        eventPublisher.publishEvent(event);
    }
    public boolean publishIsMember(String userId){
        IsMemberEvent event = new IsMemberEvent(this, userId);
        eventPublisher.publishEvent(event);
        return event.getResult();
    }
    // public void publishUnreserveTickets(String eventID, int quantity){
    //     TicketUnreservationEvent event = new TicketUnreservationEvent(this, eventID, quantity);
    //     eventPublisher.publishEvent(event);
    // }

    public boolean publishIsUpToPolicy(ActiveOrderDTO order, Integer age){
        IsUpToPolicyEvent event = new IsUpToPolicyEvent(this,order, age);
        eventPublisher.publishEvent(event);
        return event.getResult();
    }
    public void publishCompletedOrder(ActiveOrderDTO order, double amountPaid, int companyId, int transactionId, List<BarcodeDTO> barcodes){
        CompletedOrderEvent event = new CompletedOrderEvent(this, order, amountPaid, companyId, transactionId, barcodes);
        eventPublisher.publishEvent(event);
    }
    public void publishCompletedOrder(ActiveOrderDTO order, double amountPaid, int companyId, int transactionId){
        publishCompletedOrder(order, amountPaid, companyId, transactionId, null);
    }
    public void publishCompletedOrder(ActiveOrderDTO order, double amountPaid, int companyId){
        publishCompletedOrder(order, amountPaid, companyId, -1, null);
    }
    public Integer publishGetCompanyId(String eventId){
        GetCompanyIdEvent event = new GetCompanyIdEvent(this, eventId);
        eventPublisher.publishEvent(event);
        return event.getResult();
    }
    public void publishOrderCancelled(String userId, String orderId) {
        OrderCancelledEvent event = new OrderCancelledEvent(this, userId, orderId);
        eventPublisher.publishEvent(event);
    }

    public void publishRefund(String userId, String orderId, double amount) {
        OrderRefundedEvent event = new OrderRefundedEvent(this, userId, orderId, amount);
        eventPublisher.publishEvent(event);
    }

    public List<String> publishCheckSeatsReserved(String sessionToken, String orderId, String eventId, List<String> seatIds){
        CheckSeatsReservedEvent event = new CheckSeatsReservedEvent(this, sessionToken, orderId, eventId, seatIds);
        eventPublisher.publishEvent(event);
        return event.getResult();
    }
}
