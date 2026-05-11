package com.ticketpurchasingsystem.project.application;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import com.ticketpurchasingsystem.project.domain.Production.OptimisticLockingFailureException;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.RolesTreeDTO;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

public class ProductionService implements IProductionService {

    private final AuthenticationService authenticationService;
    private final ProductionHandler productionHandler;
    private final IProdRepo prodRepo;
    private final ProductionEventPublisher productionEventPublisher;

    public ProductionService(AuthenticationService authenticationService,
            ProductionHandler productionHandler,
            IProdRepo prodRepo,
            ProductionEventPublisher productionEventPublisher) {
        this.authenticationService = authenticationService;
        this.productionHandler = productionHandler;
        this.prodRepo = prodRepo;
        this.productionEventPublisher = productionEventPublisher;
    }

    @Override
    public boolean createProductionCompany(String sessionToken, ProductionCompanyDTO companyDetails) {
        if (!authenticationService.validate(sessionToken)) {
            return false;
        }
        String userId = authenticationService.getUser(sessionToken);

        Optional<ProductionCompany> existing = prodRepo.findByName(companyDetails.getCompanyName());
        if (existing.isPresent()) {
            loggerDef.getInstance().error("Company name already exists: " + companyDetails.getCompanyName());
            return false;
        }

        ProductionCompany company = productionHandler.createProductionCompany(userId, companyDetails);
        if (company == null) {
            return false;
        }

        try {
            ProductionCompany saved = prodRepo.save(company);
            productionEventPublisher.publishNewProdEvent(saved);
            return true;
        } catch (Exception e) {
            loggerDef.getInstance().error("Failed to save company: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean assignOwner(String sessionToken, Integer companyId, String appointeeUserId) {
        if (!authenticationService.validate(sessionToken)) {
            return false;
        }
        String appointerId = authenticationService.getUser(sessionToken);

        if (!productionEventPublisher.publishIsUserRegisteredEvent(appointeeUserId)) {
            return false;
        }

        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            Optional<ProductionCompany> companyOpt = prodRepo.findById(companyId);
            if (companyOpt.isEmpty()) {
                loggerDef.getInstance().error("assignOwner: company not found, id=" + companyId);
                return false;
            }

            ProductionCompany company = productionHandler.assignOwner(
                    appointerId, companyId, appointeeUserId, companyOpt.get());
            if (company == null) {
                return false;
            }

            try {
                ProductionCompany saved = prodRepo.save(company);
                productionEventPublisher.publishAssignOwnerEvent(saved, appointerId, appointeeUserId);
                loggerDef.getInstance().info(
                        "assignOwner: " + appointeeUserId + " appointed as owner of company "
                                + companyId + " by " + appointerId);
                return true;
            } catch (OptimisticLockingFailureException e) {
                loggerDef.getInstance().info("assignOwner: concurrent conflict, retrying (attempt " + (attempt + 1) + ")");
            } catch (Exception e) {
                loggerDef.getInstance().error("assignOwner failed: " + e.getMessage());
                return false;
            }
        }
        loggerDef.getInstance().error("assignOwner failed after " + maxRetries + " retries due to concurrent modifications");
        return false;
    }

    @Override
    public boolean appointManager(String sessionToken, Integer companyId, String managerId,
            Set<ManagerPermission> permissions) {
        if (!authenticationService.validate(sessionToken)) {
            return false;
        }
        String appointerId = authenticationService.getUser(sessionToken);

        if (!productionEventPublisher.publishIsUserRegisteredEvent(managerId)) {
            return false;
        }

        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            Optional<ProductionCompany> companyOpt = prodRepo.findById(companyId);
            if (companyOpt.isEmpty()) {
                loggerDef.getInstance().error("appointManager: company not found, id=" + companyId);
                return false;
            }

            ProductionCompany company = productionHandler.appointManager(
                    appointerId, companyId, managerId, permissions, companyOpt.get());
            if (company == null) {
                return false;
            }

            try {
                ProductionCompany saved = prodRepo.save(company);
                productionEventPublisher.publishAppointManagerEvent(saved, appointerId, managerId, permissions);
                loggerDef.getInstance().info(
                        "appointManager: " + managerId + " appointed as manager of company "
                                + companyId + " by " + appointerId);
                return true;
            } catch (OptimisticLockingFailureException e) {
                loggerDef.getInstance().info("appointManager: concurrent conflict, retrying (attempt " + (attempt + 1) + ")");
            } catch (Exception e) {
                loggerDef.getInstance().error("appointManager failed: " + e.getMessage());
                return false;
            }
        }
        loggerDef.getInstance().error("appointManager failed after " + maxRetries + " retries due to concurrent modifications");
        return false;
    }

    @Override
    public List<HistoryOrderItem> getCompanyPurchaseHistory(String sessionToken, Integer companyId) {
        if (!authenticationService.validate(sessionToken)) {
            loggerDef.getInstance().error("getCompanyPurchaseHistory: invalid session token");
            return null;
        }
        String userId = authenticationService.getUser(sessionToken);

        Optional<ProductionCompany> companyOpt = prodRepo.findById(companyId);
        if (companyOpt.isEmpty()) {
            loggerDef.getInstance().error("getCompanyPurchaseHistory: company not found, id=" + companyId);
            return null;
        }

        if (!(productionHandler.validateOwnerAccess(userId, companyOpt.get())
                || productionHandler.validateFounderAccess(userId, companyOpt.get()))) {
            return null;
        }

        List<HistoryOrderItem> history = productionEventPublisher.publishGetCompanyHistoryEvent(companyId);
        return history != null ? history : Collections.emptyList();
    }

    @Override
    public boolean modifyManagerPermissions(String sessionToken, Integer companyId,
            String managerId, Set<ManagerPermission> permissions) {
        if (!authenticationService.validate(sessionToken)) {
            return false;
        }
        String ownerId = authenticationService.getUser(sessionToken);

        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            Optional<ProductionCompany> companyOpt = prodRepo.findById(companyId);
            if (companyOpt.isEmpty()) {
                loggerDef.getInstance().error("modifyManagerPermissions: company not found, id=" + companyId);
                return false;
            }

            ProductionCompany company = productionHandler.modifyManagerPermissions(
                    ownerId, companyId, managerId, permissions, companyOpt.get());
            if (company == null) {
                return false;
            }

            try {
                ProductionCompany saved = prodRepo.save(company);
                productionEventPublisher.publishModifyManagerPermissionsEvent(saved, ownerId, managerId, permissions);
                loggerDef.getInstance().info(
                        "modifyManagerPermissions: permissions updated for manager " + managerId
                                + " in company " + companyId + " by " + ownerId);
                return true;
            } catch (OptimisticLockingFailureException e) {
                loggerDef.getInstance().info("modifyManagerPermissions: concurrent conflict, retrying (attempt " + (attempt + 1) + ")");
            } catch (Exception e) {
                loggerDef.getInstance().error("modifyManagerPermissions failed: " + e.getMessage());
                return false;
            }
        }
        loggerDef.getInstance().error("modifyManagerPermissions failed after " + maxRetries + " retries due to concurrent modifications");
        return false;
    }
    @Override
    public RolesTreeDTO getRolesTree(String sessionToken, Integer companyId) {
        if (!authenticationService.validate(sessionToken)) {
            loggerDef.getInstance().error("getRolesTree: invalid session token");
            return null;
        }
        String userId = authenticationService.getUser(sessionToken);

        Optional<ProductionCompany> companyOpt = prodRepo.findById(companyId);
        if (companyOpt.isEmpty()) {
            loggerDef.getInstance().error("getRolesTree: company not found, id=" + companyId);
            return null;
        }

        RolesTreeDTO result = productionHandler.getRolesTree(userId, companyOpt.get());
        if (result == null) {
            loggerDef.getInstance().error(
                    "getRolesTree: user " + userId + " is not authorized or fetch failed for company " + companyId);
            return null;
        }

        loggerDef.getInstance().info(
                "getRolesTree: roles tree fetched successfully for company " + companyId + " by user " + userId);
        return result;
    }

    @Override
    public void createEvent(String eventName, String eventDate, String eventLocation, int totalTickets, String userId) {
        throw new UnsupportedOperationException("Unimplemented method 'createEvent'");
    }

    @Override
    public void updateEvent(String eventId, String eventName, String eventDate, String eventLocation,
            int totalTickets, String userId) {
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
