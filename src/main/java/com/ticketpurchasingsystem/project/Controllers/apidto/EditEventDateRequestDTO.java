package com.ticketpurchasingsystem.project.Controllers.apidto;

import java.time.LocalDateTime;

public class EditEventDateRequestDTO {

    private LocalDateTime newDateTime;

    public EditEventDateRequestDTO() {}

    public LocalDateTime getNewDateTime() { return newDateTime; }
    public void setNewDateTime(LocalDateTime newDateTime) { this.newDateTime = newDateTime; }
}
