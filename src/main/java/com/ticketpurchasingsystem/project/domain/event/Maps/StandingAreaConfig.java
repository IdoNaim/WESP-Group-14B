package com.ticketpurchasingsystem.project.domain.event;

public class StandingAreaConfig {
    public int capacity;
    public double price;

    public StandingAreaConfig(int capacity, double price) {
        this.capacity = capacity;
        this.price = price;
    }

    public int getCapacity() {
        return capacity;
    }
    public double getPrice() {
        return price;
    }
}
