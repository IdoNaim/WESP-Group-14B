package com.ticketpurchasingsystem.project.domain.event;

public class SeatingAreaConfig {
    public int rows;
    public int seatsPerRow;
    public double price;

    public SeatingAreaConfig(int rows, int seatsPerRow, double price) {
        this.rows = rows;
        this.seatsPerRow = seatsPerRow;
        this.price = price;
    }
}
