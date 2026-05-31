
package com.ticketpurchasingsystem.project.infrastructure;

import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.application.IBarCodeGateway;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.BarcodeDTO;

import java.util.List;

@Component
public class BarCodeGateway implements IBarCodeGateway {

    @Override
    public List<BarcodeDTO> issueBarcodes(ActiveOrderDTO activeOrder) {
        List<BarcodeDTO> barcodes = new java.util.ArrayList<>();
        for (String seatId : activeOrder.getSeatIds()) {
            barcodes.add(new BarcodeDTO(activeOrder.getOrderId() + "-" + seatId));
        }
        activeOrder.getStandingAreaQuantities().forEach((areaId, qty) -> {
            for (int i = 0; i < qty; i++) {
                barcodes.add(new BarcodeDTO(activeOrder.getOrderId() + "-" + areaId + "-" + i));
            }
        });
        return barcodes;
    }
}