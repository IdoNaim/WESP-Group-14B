package com.ticketpurchasingsystem.project.Controllers.apidto;

import java.util.List;

public class AddSeatsRequestDTO {
    private List<String> seatIds;

    public List<String> getSeatIds() { return seatIds; }
    public void setSeatIds(List<String> seatIds) { this.seatIds = seatIds; }
}
