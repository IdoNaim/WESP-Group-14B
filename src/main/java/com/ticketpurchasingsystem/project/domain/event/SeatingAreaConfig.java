package com.ticketpurchasingsystem.project.domain.event;

public class SeatingAreaConfig {
    private int rows;
    private int seatsPerRow;
    private double price;

    public SeatingAreaConfig(int rows, int seatsPerRow, double price) {
        this.rows = rows;
        this.seatsPerRow = seatsPerRow;
        this.price = price;
    }

    public int getRows() {
        return rows;
    }
    public int getseatsPerRow() {
        return seatsPerRow;
    }
    public double getPrice() {
        return price;
    }
}
