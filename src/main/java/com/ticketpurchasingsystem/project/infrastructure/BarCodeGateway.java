
package com.ticketpurchasingsystem.project.infrastructure;

import com.ticketpurchasingsystem.project.application.IBarCodeGateway;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.BarcodeDTO;

import java.util.List;

public class BarCodeGateway implements IBarCodeGateway {

    @Override
    public List<BarcodeDTO> issueBarcodes(ActiveOrderDTO activeOrder) {
        //TODO: implement
        return null;
    }
}