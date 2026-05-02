
package com.ticketpurchasingsystem.project.domain.event;

public class StandingArea implements Bookable{
    private String areaId;
    private int avalibleSeats;
    private int capacity;
    private double priceForTicket;


    public StandingArea(int capacity, double priceForTicket, String areaId) {
        this.capacity = capacity;
        this.priceForTicket = priceForTicket;
        this.avalibleSeats = capacity;
        this.areaId = areaId;
    }
    @Override
    public String getId(){
        return areaId;
    }
    @Override
    public boolean book(String orderId, int numberOfTickets) {
        if(isBookeable(numberOfTickets)) {
            avalibleSeats -= numberOfTickets;
            return true;
        }
        return false;
    }
    private boolean isBookeable(int numberOfTickets) {
        return avalibleSeats - numberOfTickets >= 0;
    }

    @Override
    public double getPriceForTicket() {
        return priceForTicket;
    }
    
    public int getAvalibleSeatNumber() {
        return avalibleSeats;
    }
    public boolean unbook(int numberOfTickets) {
        if (numberOfTickets <= 0 || avalibleSeats + numberOfTickets > capacity) {
            return false;
        }
        avalibleSeats += numberOfTickets;
        return true;
    }
    public boolean setPriceForTicket(double newPrice) {
        if (newPrice < 0) {
            return false;
        }
        priceForTicket = newPrice;
        return true;
    }
}