package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;
import org.springframework.context.ApplicationEvent;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;
public class IsDiscountEvent extends ApplicationEvent{
    private ActiveOrderItem order;
    private Discount discount; // The listener will set this
    public IsDiscountEvent(Object source, ActiveOrderItem order) {
        super(source);
        this.order = order;
        this.discount = null;
    }
    public Discount getDiscount() {
        return discount;
    }
    public void setDiscount(Discount discount) {
        this.discount = discount;
    }
    public ActiveOrderItem getOrder() {
        return order;
    }
}
    
