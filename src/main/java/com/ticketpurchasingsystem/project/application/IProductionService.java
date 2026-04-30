package com.ticketpurchasingsystem.project.application;

public interface IProductionService {
    public void createEvent(String eventName, String eventDate, String eventLocation, int totalTickets, String userId);

    public void updateEvent(String eventId, String eventName, String eventDate, String eventLocation, int totalTickets,
            String userId);

    public void deleteEvent(String eventId, String userId);

    public String getEventAsManager(String eventId, String userId);

    public String getAllEventsAsManager(String userId);

    public String getEventAsCustomer(String eventId);

    public boolean createProductionCompany(String sessionToken,
            com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO companyDetails);
}
