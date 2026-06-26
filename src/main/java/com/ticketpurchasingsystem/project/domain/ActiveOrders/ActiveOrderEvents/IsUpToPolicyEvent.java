package com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import org.springframework.context.ApplicationEvent;

import java.util.HashMap;
import java.util.List;

public class IsUpToPolicyEvent extends ApplicationEvent {
    private ActiveOrderDTO order;
    private Boolean result;
    private Integer age= null;
    public IsUpToPolicyEvent(Object source, ActiveOrderDTO order, Integer age){
        super(source);
        this.order = order;
        this.result = null;
        this.age = age;
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

    public int getTotalTickets(){
        int total = order.getSeatIds().size();
        for (Integer quantity : order.getStandingAreaQuantities().values()) {
            total += quantity;
        }
        return total;
    }

    public Integer getAge()
    {
       return age;
    }

    public boolean isSeatEmpty(){
        for (String seatId : order.getSeatIds()) {
            if (seatId != null && !seatId.isEmpty()) {
                return false; // Found a non-empty seat ID
            }
        }
        return true; // All seat IDs are empty
    }
}
