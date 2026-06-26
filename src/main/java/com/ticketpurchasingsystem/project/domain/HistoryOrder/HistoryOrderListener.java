package com.ticketpurchasingsystem.project.domain.HistoryOrder;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.application.IHistoryOrderService;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.BarcodeDTO;
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
        event.setResult(historyOrderRepo.findAllByCompanyId(event.getCompanyId()));
    }

    @EventListener
    public void onCompletedOrder(CompletedOrderEvent event){
        ActiveOrderDTO order = event.getOrder();
        List<String> barcodeValues = event.getBarcodes() != null
                ? event.getBarcodes().stream().map(BarcodeDTO::getBarcodeValue).collect(Collectors.toList())
                : java.util.Collections.emptyList();
        historyOrderService.createHistoryOrder(order.getOrderId(), order.getUserId(), order.getEventId(), event.getCompanyId(), Timestamp.from(java.time.Instant.now()), event.getAmountPaid(), order.getSeatIds(), order.getStandingAreaQuantities(), event.getTransactionId(), barcodeValues);
    }
}
