package com.ticketpurchasingsystem.project.application;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.BarcodeDTO;

import java.util.List;

public interface IBarCodeGateway {


    public List<BarcodeDTO> issueBarcodes(ActiveOrderDTO activeOrder);
}
