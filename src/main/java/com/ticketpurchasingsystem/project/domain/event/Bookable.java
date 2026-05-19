package com.ticketpurchasingsystem.project.domain.event;

public interface Bookable {
    public String getId();
    public boolean book(String orderId, int numberOfTickets);
    public double getPriceForTicket();
    public boolean unbook(int numberOfTickets);
    public boolean setPriceForTicket(double newPrice);
}