package com.ticketpurchasingsystem.project.application;

import java.util.List;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.BarcodeDTO;

public interface IBarCodeGateway {
    List<BarcodeDTO> issueBarcodes(ActiveOrderDTO activeOrder);
    void cancelTickets(List<BarcodeDTO> barcodes);
}
