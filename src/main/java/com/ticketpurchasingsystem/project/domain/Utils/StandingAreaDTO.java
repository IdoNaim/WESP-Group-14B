package com.ticketpurchasingsystem.project.domain.Utils;

public class StandingAreaDTO {
    private String areaId;
    private int availableSeats;
    private int capacity;
    private double priceForTicket;
    public StandingAreaDTO(String areaId, int availableSeats, int capacity, double priceForTicket) {
        this.areaId = areaId;
        this.availableSeats = availableSeats;
        this.capacity = capacity;
        this.priceForTicket = priceForTicket;
    }
    public String getAreaId() {
        return areaId;
    }
    public int getAvailableSeats() {
        return availableSeats;
    }
    public int getCapacity() {
        return capacity;
    }
    public double getPriceForTicket() {
        return priceForTicket;
    }
    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }
    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    public void setPriceForTicket(double priceForTicket) {
        this.priceForTicket = priceForTicket;
    }
}
