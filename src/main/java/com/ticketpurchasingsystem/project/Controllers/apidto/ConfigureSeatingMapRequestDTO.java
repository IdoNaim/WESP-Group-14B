package com.ticketpurchasingsystem.project.Controllers.apidto;

import java.util.List;

public class ConfigureSeatingMapRequestDTO {

    private List<SeatingAreaDTO> seatingAreas;
    private List<StandingAreaDTO> standingAreas;

    public ConfigureSeatingMapRequestDTO() {}

    public List<SeatingAreaDTO> getSeatingAreas() { return seatingAreas; }
    public void setSeatingAreas(List<SeatingAreaDTO> seatingAreas) { this.seatingAreas = seatingAreas; }

    public List<StandingAreaDTO> getStandingAreas() { return standingAreas; }
    public void setStandingAreas(List<StandingAreaDTO> standingAreas) { this.standingAreas = standingAreas; }

    public static class SeatingAreaDTO {
        private int rows;
        private int seatsPerRow;
        private double price;

        public SeatingAreaDTO() {}

        public int getRows() { return rows; }
        public void setRows(int rows) { this.rows = rows; }

        public int getSeatsPerRow() { return seatsPerRow; }
        public void setSeatsPerRow(int seatsPerRow) { this.seatsPerRow = seatsPerRow; }

        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
    }

    public static class StandingAreaDTO {
        private int capacity;
        private double price;

        public StandingAreaDTO() {}

        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }

        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
    }
}
