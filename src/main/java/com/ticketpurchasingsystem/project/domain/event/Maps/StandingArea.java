package com.ticketpurchasingsystem.project.domain.event.Maps;

import jakarta.persistence.*;

@Entity
@Table(name = "EventsStandingAres")
@IdClass(StandingAreaId.class) // Links your standing area composite key class
public class StandingArea {

    @Id
    @Column(name = "eventId", insertable = false, updatable = false)
    private String eventId;

    @Id
    @Column(name = "areaId")
    private String areaId;

    @Column(name = "availableSeats", nullable = false)
    private int availableSeats;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "price", nullable = false)
    private double price;
//    private String desc;


    public StandingArea() {}
    public StandingArea(int capacity, double priceForTicket, String areaId) {
        this.capacity = capacity;
        this.price = priceForTicket;
        this.capacity = capacity;
//        this.desc = desc;
        this.areaId = areaId;
    }
    public String getId(){
        return areaId;
    }

    public boolean book(String orderId, int numberOfTickets) {
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
    public boolean unbook(int numberOfTickets) {
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
//    public String getDesc(){
//        return desc;
//    }
//    public void setDesc(String newDesc){
//        desc = newDesc;
//    }
}