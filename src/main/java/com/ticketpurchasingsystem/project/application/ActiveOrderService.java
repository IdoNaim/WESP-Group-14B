package com.ticketpurchasingsystem.project.application;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
//import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.IsValidEventIDEvent;
import com.ticketpurchasingsystem.project.domain.event.EventDiscountPolicy;
public class ActiveOrderService implements IActiveOrderService {
    ActiveOrderListener activeOrderListener;
    ActiveOrderPublisher activeOrderPublisher;
    IActiveOrderRepo activeOrderRepo;
    public ActiveOrderService(ActiveOrderListener activeOrderListener, ActiveOrderPublisher activeOrderPublisher, IActiveOrderRepo activeOrderRepo) {
        this.activeOrderListener = activeOrderListener;
        this.activeOrderPublisher = activeOrderPublisher;
        this.activeOrderRepo = activeOrderRepo;
    }
    @Override
    public String createActiveOrder(String userId, int eventId, int quantity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void cancelActiveOrder(int orderId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void getActiveOrders(String userId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void getActiveOrder(int orderId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void completeActiveOrder(int orderId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateActiveOrder(int orderId, int quantity) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public boolean saveOrder(ActiveOrderItem order) {
        try{
            if(order == null){
                System.out.println("Order cannot be null");
                return false;
            }
            if(order.getQuantity() <= 0){
                System.out.println("Quantity must be greater than 0");
                return false;
            }
            if(!isValidEventID(order.getEventId()) || !isValidOrderID(order.getOrderId())) {
                System.out.println("bad order ID or event ID");
                return false;
            }
        return activeOrderRepo.save(order);
        }catch(Exception e){

            return false;
        }
    }
    private boolean isValidEventID(int eventId) {
        
        return activeOrderPublisher.publishIsValidEventIDEvent(eventId);
    }
    private boolean isValidOrderID(int orderId) {
        
        return orderId > 0;
    }
    
    
    public Discount isDiscount(int orderId) {
        ActiveOrderItem order = activeOrderRepo.findById(orderId);
        if(order == null){
            System.out.println("Order not found");
            return null;
        }

        return activeOrderPublisher.publishIsDiscountEvent(order);
    }

    private boolean activeOrderExists(int orderId) {
        return activeOrderRepo.findById(orderId) != null;
    }
}
