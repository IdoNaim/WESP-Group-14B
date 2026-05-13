package com.ticketpurchasingsystem.project.domain.HistoryOrder;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.application.IHistoryOrderService;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.CompletedOrderEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.GetCompanyHistoryEvent;
import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllHistoryOrdersEvent;

@Component
public class HistoryOrderListener {

    private final IHistoryOrderRepo historyOrderRepo;
    private final IHistoryOrderService historyOrderService;

    public HistoryOrderListener(IHistoryOrderRepo historyOrderRepo, IHistoryOrderService historyOrderService) {
        this.historyOrderRepo = historyOrderRepo;
        this.historyOrderService = historyOrderService;
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

    @EventListener
    public void onCompletedpublishedOrder(CompletedOrderEvent event){
        historyOrderService.createHistoryOrder(event.getSessionToken(), event.getOrderId(), event.getUserId(), event.getEventId(), event.getCompanyId(), event.getPurchaseDate(), event.getPrice(), event.getSeatIds(), event.getStandingAreaQuantities());
    }
}
