package com.ticketpurchasingsystem.project.application;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
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
    public String createActiveOrder(int userId, int eventId, int quantity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void cancelActiveOrder(int orderId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void getActiveOrders(int userId) {
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
        return activeOrderRepo.save(order);
        }catch(Exception e){

            return false;
        }
    }
    
}
