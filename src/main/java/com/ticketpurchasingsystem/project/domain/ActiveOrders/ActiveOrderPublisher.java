package com.ticketpurchasingsystem.project.domain.ActiveOrders;
import com.ticketpurchasingsystem.project.domain.Utils.Publisher;
import org.springframework.context.ApplicationEventPublisher;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.*;
public class ActiveOrderPublisher {
    private ApplicationEventPublisher eventPublisher;
    public ActiveOrderPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    public boolean publishIsValidEventIDEvent(int eventId) {
        IsValidEventIDEvent event = new IsValidEventIDEvent(this, eventId);
        eventPublisher.publishEvent(event);
        return event.isValid();
    }
    public Discount publishIsDiscountEvent(ActiveOrderItem order) {
        IsDiscountEvent event = new IsDiscountEvent(this, order);
        eventPublisher.publishEvent(event);
        return event.getDiscount();
    }
}
