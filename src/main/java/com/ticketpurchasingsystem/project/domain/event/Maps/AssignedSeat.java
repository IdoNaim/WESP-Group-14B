package com.ticketpurchasingsystem.project.domain.event.Maps;

import jakarta.persistence.*;

@Entity
@Table(name = "EventsSeats")
public class AssignedSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Simple, auto-generated database primary key

    @Column(name = "seatId", nullable = false)
    private String seatId;

    @Column(name = "isBooked", nullable = false)
    private boolean isBooked;

    @Column(name = "orderId")
    private String orderId;

    @Column(name = "price", nullable = false)
    private double price;

    public AssignedSeat() {}

    public AssignedSeat(String zone, int row, int number, double priceForTicket) {
        // Generates the unique seat identifier string (e.g., "ZoneA_1_5")
        this.seatId = String.format("%s_%d_%d", zone, row, number);
        this.isBooked = false;
        this.orderId = null;
        this.price = priceForTicket;
    }

    public boolean isbooked(String orderId) {
        if(orderId == null){
            return false;
        }
        return orderId.equals(this.orderId);
    }

    public String getId() {
        return seatId; // Keeps your Map keys intact
    }

    public boolean isBooked() {
        return isBooked;
    }

    public synchronized boolean book(String orderId, int numberOfTickets) {
        if (numberOfTickets != 1 || isBooked) {
            return false;
        }
        this.isBooked = true;
        this.orderId = orderId;
        return true;
    }

    public synchronized boolean unbook(int numberOfTickets) {
        if(numberOfTickets != 1 || !isBooked) {
            return false;
        }
        this.isBooked = false;
        this.orderId = null;
        return true;
    }

    public double getPriceForTicket(){
        return price;
    }

    public boolean setPriceForTicket(double newPrice) {
        if (newPrice < 0) {
            return false;
        }
        this.price = newPrice;
        return true;
    }

    public String getOrderId() {
        return orderId;
    }
}