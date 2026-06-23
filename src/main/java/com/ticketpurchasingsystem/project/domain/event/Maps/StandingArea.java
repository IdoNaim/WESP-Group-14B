package com.ticketpurchasingsystem.project.domain.event.Maps;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "EventsStandingAreas")
public class StandingArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Simple, auto-generated database primary key

    @Column(name = "areaId", nullable = false)
    private String areaId;

    @Column(name = "availableSeats", nullable = false)
    private int availableSeats;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "price", nullable = false)
    private double price;

    public StandingArea() {}

    public StandingArea(int capacity, double priceForTicket, String areaId) {
        this.capacity = capacity;
        this.price = priceForTicket;
        this.availableSeats = capacity; // Fixed: Properly initializes available seats
        this.areaId = areaId;
    }

    public String getId(){
        return areaId; // Keeps your Map logic working perfectly
    }

    // Test number 2

    public synchronized boolean book(String orderId, int numberOfTickets) {
        if(isBookeable(numberOfTickets)) {
            availableSeats -= numberOfTickets;
            return true;
        }
        return false;
    }

    private boolean isBookeable(int numberOfTickets) {
        return availableSeats - numberOfTickets >= 0;
    }

    public double getPriceForTicket() {
        return price;
    }

    public int getAvalibleSeatNumber() {
        return availableSeats;
    }

    public synchronized boolean unbook(int numberOfTickets) {
        if (numberOfTickets <= 0 || availableSeats + numberOfTickets > capacity) {
            return false;
        }
        availableSeats += numberOfTickets;
        return true;
    }

    public boolean setPriceForTicket(double newPrice) {
        if (newPrice < 0) {
            return false;
        }
        price = newPrice;
        return true;
    }

    public boolean isbooked(String orderId) {
        return true;
    }

    public int getCapacity() {
        return capacity;
    }
}