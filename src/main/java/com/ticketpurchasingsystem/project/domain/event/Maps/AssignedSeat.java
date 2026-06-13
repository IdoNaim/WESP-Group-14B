package com.ticketpurchasingsystem.project.domain.event.Maps;

import jakarta.persistence.*;

@Entity
@Table(name = "EventsSeats")
@IdClass(AssignedSeatId.class)
public class AssignedSeat{

    @Id
    @Column(name = "eventId", insertable = false, updatable = false)
    private String eventId; // Mapped as part of the PK, but managed by the parent Event relationship

    @Id
    @Column(name = "seatId")
    private String seatId;

    @Column(name = "isBooked", nullable = false)
    private boolean isBooked;

    @Column(name = "orderId")
    private String orderId;

    @Column(name = "price", nullable = false)
    private double price;

    public AssignedSeat() {}
    public AssignedSeat(String zone, int row, int number, double priceForTicket) {
        this.eventId = String.format("%s_%d_%d", zone, row, number);
        this.isBooked = false;
        this.orderId = null;
        this.price = priceForTicket;
    }

    public boolean isbooked(String orderId) {
        if(orderId == null){
            return false;
        }
        else{
            return orderId.equals(this.orderId);
        }
    }

    public String getId() {
        return eventId;
    }

    public boolean isBooked() {
        return isBooked;
    }

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

    public double getPriceForTicket(){
        return price;
    }

    public boolean setPriceForTicket(double newPrice) {
        if (newPrice < 0) {
            return false; // Price cannot be negative
        }
        this.price = newPrice;
        return true; // Price updated successfully
    }

    public String getOrderId() {
        return orderId;
    }
}
