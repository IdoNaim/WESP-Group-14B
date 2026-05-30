package com.ticketpurchasingsystem.project.Controllers.apidto;

public class AddStandingAreaRequestDTO {
    private String areaId;
    private int quantity;

    public String getAreaId() { return areaId; }
    public void setAreaId(String areaId) { this.areaId = areaId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}