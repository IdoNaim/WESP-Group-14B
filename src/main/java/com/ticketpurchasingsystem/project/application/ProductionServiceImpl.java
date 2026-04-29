package com.ticketpurchasingsystem.project.application;

import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;

public class ProductionServiceImpl implements ProductionService {
    private final AuthenticationService authenticationService;
    private final ProductionHandler productionHandler;

    public ProductionServiceImpl(AuthenticationService authenticationService, ProductionHandler productionHandler) {
        this.authenticationService = authenticationService;
        this.productionHandler = productionHandler;
    }

    @Override
    public boolean createProductionCompany(String sessionToken, ProductionCompanyDTO companyDetails) {
        // Alt Flow 1: Member is not authenticated
        if (!authenticationService.validateToken(sessionToken)) {
            return false;
        }

        String userId = authenticationService.extractUsername(sessionToken);

        // Delegate to Domain Handler
        return productionHandler.createProductionCompany(userId, companyDetails);
    }

    @Override
    public void createEvent(String eventName, String eventDate, String eventLocation, int totalTickets, String userId) {
        throw new UnsupportedOperationException("Unimplemented method 'createEvent'");
    }

    @Override
    public void updateEvent(String eventId, String eventName, String eventDate, String eventLocation, int totalTickets, String userId) {
        throw new UnsupportedOperationException("Unimplemented method 'updateEvent'");
    }

    @Override
    public void deleteEvent(String eventId, String userId) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteEvent'");
    }

    @Override
    public String getEventAsManager(String eventId, String userId) {
        throw new UnsupportedOperationException("Unimplemented method 'getEventAsManager'");
    }

    @Override
    public String getAllEventsAsManager(String userId) {
        throw new UnsupportedOperationException("Unimplemented method 'getAllEventsAsManager'");
    }

    @Override
    public String getEventAsCustomer(String eventId) {
        throw new UnsupportedOperationException("Unimplemented method 'getEventAsCustomer'");
    }
}
