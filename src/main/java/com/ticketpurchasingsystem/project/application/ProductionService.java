package com.ticketpurchasingsystem.project.application;

import com.ticketpurchasingsystem.project.application.UserService.IUserService;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;

public class ProductionService implements IProductionService {
    private final AuthenticationService authenticationService;
    private final ProductionHandler productionHandler;
    private final IUserService userService;

    public ProductionService(AuthenticationService authenticationService, ProductionHandler productionHandler,
            IUserService userService) {
        this.authenticationService = authenticationService;
        this.productionHandler = productionHandler;
        this.userService = userService;
    }

    public ProductionService(AuthenticationService authenticationService,
            ProductionHandler productionHandler) {
        this(authenticationService, productionHandler, null);
    }

    @Override
    public boolean createProductionCompany(String sessionToken, ProductionCompanyDTO companyDetails) {
        if (!authenticationService.validate(sessionToken)) {
            return false;
        }
        String userId = authenticationService.getUser(sessionToken);
        return productionHandler.createProductionCompany(userId, companyDetails);
    }

    @Override
    public void createEvent(String eventName, String eventDate, String eventLocation, int totalTickets, String userId) {
        throw new UnsupportedOperationException("Unimplemented method 'createEvent'");
    }

    @Override
    public void updateEvent(String eventId, String eventName, String eventDate, String eventLocation, int totalTickets,
            String userId) {
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

    @Override
    public boolean assignOwner(String sessionToken, Integer companyId, String appointeeUserId) {
        if (!authenticationService.validate(sessionToken)) {
            return false;
        }
        String appointerId = authenticationService.getUser(sessionToken);
        if (userService.getUser(appointeeUserId) == null) {
            return false;
        }
        return productionHandler.assignOwner(appointerId, companyId, appointeeUserId);
    }
}
