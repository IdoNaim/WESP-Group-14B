package com.ticketpurchasingsystem.project.Controllers.apidto;

public class CreateOrderRequestDTO {
    private String userId;
    private String eventId;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
}
