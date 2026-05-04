package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import org.springframework.context.ApplicationEvent;

import java.util.HashMap;
import java.util.List;

public class IsUpToPolicyEvent extends ApplicationEvent {
    private ActiveOrderDTO order;
    private Boolean result;
    public IsUpToPolicyEvent(Object source, ActiveOrderDTO order){
        super(source);
        this.order = order;
        this.result = null;
    }

    public boolean getResult() {
        return result.booleanValue();
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public ActiveOrderDTO getOrder() {
        return order;
    }
    public String getEventID(){
        return order.getEventId();
    }
    public String userID(){
        return order.getUserId();
    }
    public List<String> getSeatIds(){
        return order.getSeatIds();
    }
    public HashMap<String, Integer>  getStandingAreaQuantities(){
        return order.getStandingAreaQuantities();
    }

}
