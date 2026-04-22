package com.ticketpurchasingsystem.project.infrastructure;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
import java.util.ArrayList;
import java.util.List;

public class ActiveOrderMemRepo implements IActiveOrderRepo {
    List<ActiveOrderItem> activeOrders;
    public ActiveOrderMemRepo() {
        this.activeOrders = new ArrayList<>();
    }
    @Override
    public boolean save(ActiveOrderItem order) {
        activeOrders.add(order);
        return true;
    }

    @Override
    public ActiveOrderItem findById(String orderId) {
        for(ActiveOrderItem order : activeOrders)
        {
            if(order.getOrderId() == orderId) {
                return order;
            }
        }
        return null;
    }

    @Override
    public void delete(String orderId) {
        for(ActiveOrderItem order : activeOrders) {
            if(order.getOrderId() == orderId) {
                activeOrders.remove(order);
                break;
            }
        }
    }

    @Override
    public void update(ActiveOrderItem order) {
        boolean found = false;
        for(ActiveOrderItem o : activeOrders) {
            if(o.getOrderId() == order.getOrderId()) {
                o.editOrder(order);
                found = true;
                break;
            }
        }
        if(!found) {
            activeOrders.add(order);
        }
    }
    
}
