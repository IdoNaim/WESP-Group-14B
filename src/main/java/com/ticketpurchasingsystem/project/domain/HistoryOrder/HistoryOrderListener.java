package com.ticketpurchasingsystem.project.domain.HistoryOrder;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.GetCompanyHistoryEvent;
import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllHistoryOrdersEvent;

@Component
public class HistoryOrderListener {

    private final IHistoryOrderRepo historyOrderRepo;

    public HistoryOrderListener(IHistoryOrderRepo historyOrderRepo) {
        this.historyOrderRepo = historyOrderRepo;
    }

    // SystemAdmin asks for all history orders
    @EventListener
    public void onApplicationEvent(GetAllHistoryOrdersEvent event) {
        event.setResult(historyOrderRepo.findAll());
    }

    @EventListener
    public void onGetCompanyHistory(GetCompanyHistoryEvent event) {
        event.setResult(historyOrderRepo.findByCompanyId(event.getCompanyId()));
    }
}
