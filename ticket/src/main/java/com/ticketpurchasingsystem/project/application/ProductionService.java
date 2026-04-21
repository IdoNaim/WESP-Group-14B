package com.ticketpurchasingsystem.project.application;

public interface ProductionService {
    public void createEvent(String eventName, String eventDate, String eventLocation, int totalTickets,String userId);
    public void updateEvent(String eventId, String eventName, String eventDate, String eventLocation, int totalTickets,String userId);
    public void deleteEvent(String eventId,String userId);
    public String getEventAsManager(String eventId,String userId);
    public String getAllEventsAsManager(String userId);
    public String getEventAsCustomer(String eventId);
}
