package com.ticketpurchasingsystem.project.domain.ActiveOrders;

public class BarcodeDTO {
    private String barcodeValue;
    
    public BarcodeDTO(String value){
        this.barcodeValue = value;
    }
    public String getBarcodeValue() {
        return barcodeValue;
    }
    public void setBarcodeValue(String barcodeValue) {
        this.barcodeValue = barcodeValue;
    }
}
