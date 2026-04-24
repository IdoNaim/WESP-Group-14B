package com.ticketpurchasingsystem.project.domain.ActiveOrders;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllActiveOrdersEvent;


@Component
public class ActiveOrderListener  {

    private final IActiveOrderRepo activeOrderRepo;

    public ActiveOrderListener(IActiveOrderRepo activeOrderRepo) {
        this.activeOrderRepo = activeOrderRepo;
    }

    @EventListener
    public void handleGetAllActiveOrdersEvent(GetAllActiveOrdersEvent event) {
        //Maybe add id authentication here or before publishing the event
        event.setResult(activeOrderRepo.findAll());
    }
}
