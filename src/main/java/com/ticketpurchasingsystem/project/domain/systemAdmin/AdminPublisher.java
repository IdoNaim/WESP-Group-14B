package com.ticketpurchasingsystem.project.domain.systemAdmin;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.User.UserDTO;
import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllActiveOrdersEvent;
import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllHistoryOrdersEvent;
import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllUsersEvent;

@Component
public class AdminPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public AdminPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }



    public List<ActiveOrderItem> publishGetAllActiveOrders(String reqId) {
        GetAllActiveOrdersEvent event = new GetAllActiveOrdersEvent(reqId);
        eventPublisher.publishEvent(event);
        return event.getResult();
    }

    public List<HistoryOrderItem> publishGetAllOrdersHistory(String reqId) {
        GetAllHistoryOrdersEvent event = new GetAllHistoryOrdersEvent(reqId);
        eventPublisher.publishEvent(event);
        return event.getResult();
    }


    public List<UserDTO> publishGetAllUsers(String reqId) {
        GetAllUsersEvent event = new GetAllUsersEvent(reqId);
        eventPublisher.publishEvent(event);
        return event.getResult();
    }
}
