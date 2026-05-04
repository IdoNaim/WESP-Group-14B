package com.ticketpurchasingsystem.project.domain.event;

public class AssignedSeat implements Bookable {
    private String id;              //includes zone, row and number
    private boolean isBooked;
    private String orderId;
    private double priceForTicket;

    public AssignedSeat(String zone, int row, int number, double priceForTicket) {
        this.id = String.format("%s_%d_%d", zone, row, number);
        this.isBooked = false;
        this.orderId = null;
        this.priceForTicket = priceForTicket;
    }

    @Override
    public String getId() {
        return id;
    }

    public boolean isBooked() {
        return isBooked;
    }

    @Override
    public boolean book(String orderId, int numberOfTickets) {
         if (numberOfTickets != 1) {
            return false; // Assigned seats can only be booked one at a time
        }
        if (isBooked) {
            return false; // Seat is already booked
        }
        this.isBooked = true;
        this.orderId = orderId;
        return true; // Booking successful
    }

    public boolean unbook(int numberOfTickets) {
        if(numberOfTickets != 1) {
            return false; // Assigned seats can only be unbooked one at a time
        }
        if (!isBooked) {
            return false; // Seat is not booked
        }
        this.isBooked = false;
        this.orderId = null;
        return true; // Unbooking successful
    }

    @Override
    public double getPriceForTicket(){
        return priceForTicket;
    }

    public boolean setPriceForTicket(double newPrice) {
        if (newPrice < 0) {
            return false; // Price cannot be negative
        }
        this.priceForTicket = newPrice;
        return true; // Price updated successfully
    }
    
   
}
