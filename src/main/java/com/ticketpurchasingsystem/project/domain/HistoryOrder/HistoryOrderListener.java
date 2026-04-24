package com.ticketpurchasingsystem.project.domain.HistoryOrder;


import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllHistoryOrdersEvent;


@Component
public class HistoryOrderListener  {

    private final IHistoryOrderRepo historyOrderRepo;

    public HistoryOrderListener(IHistoryOrderRepo historyOrderRepo) {
        this.historyOrderRepo = historyOrderRepo;
    }

    @EventListener
    public void onApplicationEvent(GetAllHistoryOrdersEvent event) {
        //Auth?
        event.setResult(historyOrderRepo.findAll());
    }
}
