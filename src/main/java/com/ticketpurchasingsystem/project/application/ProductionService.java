package com.ticketpurchasingsystem.project.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import com.ticketpurchasingsystem.project.domain.Production.OptimisticLockingFailureException;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Production.UserProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.Utils.CompanySummaryDTO;
import com.ticketpurchasingsystem.project.domain.Utils.MemberInfoDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PendingAppointmentDTO;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.RolesTreeDTO;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

@Service
public class ProductionService implements IProductionService {

    private final AuthenticationService authenticationService;
    private final ProductionHandler productionHandler;
    private final IProdRepo prodRepo;
    private final ProductionEventPublisher productionEventPublisher;

    @Autowired
    public ProductionService(AuthenticationService authenticationService,
            ProductionHandler productionHandler,
            IProdRepo prodRepo,
            ProductionEventPublisher productionEventPublisher) {
        this.authenticationService = authenticationService;
        this.productionHandler = productionHandler;
        this.prodRepo = prodRepo;
        this.productionEventPublisher = productionEventPublisher;
        // ProductionCompanyDTO testCompanyDTO = new ProductionCompanyDTO("Test
        // Company", "Test Description", "comp@gmail.com");
        // ProductionCompany testCompany =
        // productionHandler.createProductionCompany("idonaim56@gmail.com",
        // testCompanyDTO);
        // if(testCompany == null) {
        // loggerDef.getInstance().error("Failed to create test company");
        // return;
        // }
        // prodRepo.save(testCompany);
        // loggerDef.getInstance().info("Test company created and saved.");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Integer createProductionCompany(String sessionToken, ProductionCompanyDTO companyDetails) {
        if (!authenticationService.validate(sessionToken)) {
            loggerDef.getInstance().error("createProductionCompany: invalid session token");
            return null;
        }
        String userId = authenticationService.getUser(sessionToken);

        Optional<ProductionCompany> existing = prodRepo.findByName(companyDetails.getCompanyName());
        if (existing.isPresent()) {
            loggerDef.getInstance().error("Company name already exists: " + companyDetails.getCompanyName());
            return null;
        }

        ProductionCompany company = productionHandler.createProductionCompany(userId, companyDetails);
        if (company == null) {
            return null;
        }

        try {
            ProductionCompany saved = prodRepo.save(company);
            productionEventPublisher.publishNewProdEvent(saved);
            return saved.getCompanyId();
        } catch (Exception e) {
            loggerDef.getInstance().error("Failed to save company: " + e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
                productionEventPublisher.publishAppointmentRequestedEvent(
                        saved.getCompanyId(), saved.getCompanyName(), appointeeUserId, appointerId, "OWNER");
                loggerDef.getInstance().info(
                        "assignOwner: owner appointment request sent to " + appointeeUserId
                                + " for company " + companyId + " by " + appointerId);
                return true;
            } catch (OptimisticLockingFailureException | org.springframework.dao.OptimisticLockingFailureException e) {
                loggerDef.getInstance()
                        .info("assignOwner: concurrent conflict, retrying (attempt " + (attempt + 1) + ")");
            } catch (Exception e) {
                loggerDef.getInstance().error("assignOwner failed: " + e.getMessage());
                return false;
            }
        }
        loggerDef.getInstance()
                .error("assignOwner failed after " + maxRetries + " retries due to concurrent modifications");
        return false;
    }

    @Override
    @Transactional
    public boolean appointManager(String sessionToken, Integer companyId, String managerId, Set<ManagerPermission> permissions) {
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
                productionEventPublisher.publishAppointmentRequestedEvent(
                        saved.getCompanyId(), saved.getCompanyName(), managerId, appointerId, "MANAGER");
                loggerDef.getInstance().info(
                        "appointManager: manager appointment request sent to " + managerId
                                + " for company " + companyId + " by " + appointerId);
                return true;
                
            } catch (OptimisticLockingFailureException | org.springframework.dao.OptimisticLockingFailureException e) {
                loggerDef.getInstance()
                        .info("appointManager: concurrent conflict, retrying (attempt " + (attempt + 1) + ")");
            } catch (Exception e) {
                loggerDef.getInstance().error("appointManager failed: " + e.getMessage());
                return false;
            }
        }
        loggerDef.getInstance()
                .error("appointManager failed after " + maxRetries + " retries due to concurrent modifications");
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingAppointmentDTO> getMyPendingAppointments(String sessionToken) {
        if (!authenticationService.validate(sessionToken)) {
            return null;
        }
        String userId = authenticationService.getUser(sessionToken);
        List<ProductionCompany> companies = prodRepo.findAllWithPendingAppointee(userId);
        List<PendingAppointmentDTO> result = new ArrayList<>();
        for (ProductionCompany c : companies) {
            c.getPendingRole(userId).ifPresent(role -> result.add(new PendingAppointmentDTO(
                    c.getCompanyId(),
                    c.getCompanyName(),
                    role.name(),
                    c.getPendingAppointerId(userId).orElse(null),
                    c.getPendingPermissions(userId))));
        }
        return result;
    }

    @Override
    @Transactional
    public boolean acceptAppointment(String sessionToken, Integer companyId) {
        if (!authenticationService.validate(sessionToken)) {
            return false;
        }
        String userId = authenticationService.getUser(sessionToken);

        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            Optional<ProductionCompany> companyOpt = prodRepo.findById(companyId);
            if (companyOpt.isEmpty()) {
                loggerDef.getInstance().error("acceptAppointment: company not found, id=" + companyId);
                return false;
            }
            ProductionCompany loaded = companyOpt.get();

            // Capture the pending request details before flipping it to ACTIVE so we can
            // fire the right role event (assigns the User-aggregate role only on acceptance).
            Optional<UserProductionCompany.MemberRole> pendingRole = loaded.getPendingRole(userId);
            String appointerId = loaded.getPendingAppointerId(userId).orElse(null);
            Set<ManagerPermission> permissions = loaded.getPendingPermissions(userId);

            ProductionCompany company = productionHandler.acceptAppointment(userId, loaded);
            if (company == null) {
                return false;
            }

            try {
                ProductionCompany saved = prodRepo.save(company);
                if (pendingRole.orElse(null) == UserProductionCompany.MemberRole.OWNER) {
                    productionEventPublisher.publishAssignOwnerEvent(saved, appointerId, userId);
                } else {
                    productionEventPublisher.publishAppointManagerEvent(saved, appointerId, userId, permissions);
                }
                loggerDef.getInstance().info(
                        "acceptAppointment: " + userId + " accepted appointment in company " + companyId);
                return true;
            } catch (OptimisticLockingFailureException | org.springframework.dao.OptimisticLockingFailureException e) {
                loggerDef.getInstance().info("acceptAppointment: concurrent conflict, retrying (attempt " + (attempt + 1) + ")");
            } catch (Exception e) {
                loggerDef.getInstance().error("acceptAppointment failed: " + e.getMessage());
                return false;
            }
        }
        loggerDef.getInstance().error("acceptAppointment failed after " + maxRetries + " retries due to concurrent modifications");
        return false;
    }

    @Override
    @Transactional
    public boolean denyAppointment(String sessionToken, Integer companyId) {
        if (!authenticationService.validate(sessionToken)) {
            return false;
        }
        String userId = authenticationService.getUser(sessionToken);

        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            Optional<ProductionCompany> companyOpt = prodRepo.findById(companyId);
            if (companyOpt.isEmpty()) {
                loggerDef.getInstance().error("denyAppointment: company not found, id=" + companyId);
                return false;
            }

            ProductionCompany company = productionHandler.denyAppointment(userId, companyOpt.get());
            if (company == null) {
                return false;
            }

            try {
                prodRepo.save(company);
                loggerDef.getInstance().info(
                        "denyAppointment: " + userId + " denied appointment in company " + companyId);
                return true;
            } catch (OptimisticLockingFailureException | org.springframework.dao.OptimisticLockingFailureException e) {
                loggerDef.getInstance().info("denyAppointment: concurrent conflict, retrying (attempt " + (attempt + 1) + ")");
            } catch (Exception e) {
                loggerDef.getInstance().error("denyAppointment failed: " + e.getMessage());
                return false;
            }
        }
        loggerDef.getInstance().error("denyAppointment failed after " + maxRetries + " retries due to concurrent modifications");
        return false;
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional
    public boolean modifyManagerPermissions(String sessionToken, Integer companyId,
            String targetUserId, Set<ManagerPermission> permissions) {
        if (!authenticationService.validate(sessionToken)) {
            return false;
        }
        String requesterId = authenticationService.getUser(sessionToken);

        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            Optional<ProductionCompany> companyOpt = prodRepo.findById(companyId);
            if (companyOpt.isEmpty()) {
                loggerDef.getInstance().error("modifyManagerPermissions: company not found, id=" + companyId);
                return false;
            }

            ProductionCompany company = productionHandler.modifyManagerPermissions(
                    requesterId, companyId, targetUserId, permissions, companyOpt.get());
            
            if (company == null) {
                return false; 
            }
            
            try {
                ProductionCompany saved = prodRepo.save(company);
                
                productionEventPublisher.publishModifyManagerPermissionsEvent(saved, requesterId, targetUserId, permissions);
                
                return true;
            } catch (OptimisticLockingFailureException | org.springframework.dao.OptimisticLockingFailureException e) {
                loggerDef.getInstance().info(
                        "modifyManagerPermissions: concurrent conflict, retrying (attempt " + (attempt + 1) + ")");
            } catch (Exception e) {
                loggerDef.getInstance().error("modifyManagerPermissions failed: " + e.getMessage());
                return false;
            }
        }
        loggerDef.getInstance().error(
                "modifyManagerPermissions failed after " + maxRetries + " retries due to concurrent modifications");
        return false;
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional
    public boolean removeManager(String sessionToken, Integer companyId, String managerId) {
        if (!authenticationService.validate(sessionToken)) {
            return false;
        }
        String ownerId = authenticationService.getUser(sessionToken);
        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {

            Optional<ProductionCompany> companyOpt = prodRepo.findById(companyId);
            if (companyOpt.isEmpty()) {
                loggerDef.getInstance().error("removeManager: company not found, id=" + companyId);
                return false;
            }
            ProductionCompany company = productionHandler.removeManager(ownerId, companyId, managerId,
                    new ProductionCompany(companyOpt.get()));
            if (company == null) {
                return false;
            }
            try {
                ProductionCompany saved = prodRepo.save(company);
                loggerDef.getInstance()
                        .info("removed manager " + managerId + " from company " + companyId + " by " + ownerId);
                return true;
            } catch (OptimisticLockingFailureException | org.springframework.dao.OptimisticLockingFailureException e) {
                loggerDef.getInstance()
                        .info("removeManager: concurrent conflict, retrying (attempt " + (attempt + 1) + ")");
            } catch (Exception e) {
                loggerDef.getInstance().error("removeManager failed: " + e.getMessage());
                return false;
            }
        }
        loggerDef.getInstance()
                .error("removeManager failed after " + maxRetries + " retries due to concurrent modifications");
        return false;
    }

    @Override
    @Transactional
    public boolean removeOwner(String sessionToken, Integer companyId, String ownerId) {
        if (!authenticationService.validate(sessionToken))
            return false;
        String requesterId = authenticationService.getUser(sessionToken);
        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            Optional<ProductionCompany> companyOpt = prodRepo.findById(companyId);
            if (companyOpt.isEmpty()) {
                loggerDef.getInstance().error("removeOwner: company not found, id=" + companyId);
                return false;
            }
            ProductionCompany company = productionHandler.removeOwner(requesterId, companyId, ownerId,
                    new ProductionCompany(companyOpt.get()));
            if (company == null)
                return false;
            try {
                prodRepo.save(company);
                loggerDef.getInstance()
                        .info("removed owner " + ownerId + " from company " + companyId + " by " + requesterId);
                return true;
            } catch (OptimisticLockingFailureException | org.springframework.dao.OptimisticLockingFailureException e) {
                loggerDef.getInstance()
                        .info("removeOwner: concurrent conflict, retrying (attempt " + (attempt + 1) + ")");
            } catch (Exception e) {
                loggerDef.getInstance().error("removeOwner failed: " + e.getMessage());
                return false;
            }
        }
        loggerDef.getInstance().error("removeOwner failed after " + maxRetries + " retries");
        return false;
    }

    @Override
    @Transactional
    public boolean setCompanyPurchasePolicy(String sessionToken, Integer companyId, PurchasePolicyDTO dto) {
        if (!authenticationService.validate(sessionToken))
            return false;
        String userId = authenticationService.getUser(sessionToken);

        Optional<ProductionCompany> companyOpt = prodRepo.findById(companyId);
        if (companyOpt.isEmpty())
            return false;
        ProductionCompany company = companyOpt.get();

        if (!company.isOwner(userId) && !company.isFounder(userId))
            return false;

        company.setPurchasePolicy(dto);
        try {
            prodRepo.save(company);
            loggerDef.getInstance().info("setCompanyPurchasePolicy: policy saved for company " + companyId);
            return true;
        } catch (Exception e) {
            loggerDef.getInstance().error("setCompanyPurchasePolicy failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PurchasePolicyDTO getCompanyPurchasePolicy(String sessionToken, Integer companyId) {
        if (!authenticationService.validate(sessionToken))
            return null;
        Optional<ProductionCompany> companyOpt = prodRepo.findById(companyId);
        if (companyOpt.isEmpty())
            return null;
        return companyOpt.get().getPurchasePolicyDTO();
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

    @Override
    @Transactional(readOnly = true)
    public List<CompanySummaryDTO> getMyCompanies(String sessionToken) {
        if (!authenticationService.validate(sessionToken)) {
            return null;
        }
        String userId = authenticationService.getUser(sessionToken);
        List<ProductionCompany> companies = prodRepo.findAllByUserId(userId);
        List<CompanySummaryDTO> result = new ArrayList<>();
        for (ProductionCompany c : companies) {
            String role;
            if (userId.equals(c.getFounderId())) {
                role = "FOUNDER";
            } else if (c.isOwner(userId)) {
                role = "OWNER";
            } else {
                role = "MANAGER";
            }
            result.add(new CompanySummaryDTO(c.getCompanyId(), c.getCompanyName(), c.getCompanyDescription(),
                    c.getCompanyEmail(), role));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public MemberInfoDTO getMyMemberInfo(String sessionToken, Integer companyId) {
        if (!authenticationService.validate(sessionToken))
            return null;
        String userId = authenticationService.getUser(sessionToken);
        Optional<ProductionCompany> opt = prodRepo.findById(companyId);
        if (opt.isEmpty())
            return null;
        ProductionCompany company = opt.get();

        String role;
        Set<ManagerPermission> perms = java.util.Collections.emptySet();
        if (company.isFounder(userId)) {
            role = "FOUNDER";
        } else if (company.isOwner(userId)) {
            role = "OWNER";
        } else if (company.isManager(userId)) {
            role = "MANAGER";
            perms = company.getManagerPermissions(userId);
        } else {
            return null;
        }

        // Build per-manager permissions map
        java.util.Map<String, Set<ManagerPermission>> managerPerms = new java.util.LinkedHashMap<>();
        for (String mid : company.getManagerTree().keySet()) {
            managerPerms.put(mid, company.getManagerPermissions(mid));
        }

        return new MemberInfoDTO(role, perms, company.getCompanyName(),
                company.getFounderId(),
                company.getOwnershipTree(),
                company.getManagerTree(),
                managerPerms);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean addPurchasePolicyRule(String sessionToken, Integer companyId, IPurchaseRule rule) {
        if (!authenticationService.validate(sessionToken)) {
            return false;
        }
        String userId = authenticationService.getUser(sessionToken);

        Optional<ProductionCompany> companyOpt = prodRepo.findById(companyId);
        if (companyOpt.isEmpty()) {
            loggerDef.getInstance().error("addPurchasePolicyRule: company not found, id=" + companyId);
            return false;
        }

        ProductionCompany company = productionHandler.addPurchasePolicyRule(userId, companyId, rule, companyOpt.get());
        if (company == null) {
            return false;
        }

        try {
            prodRepo.save(company);
            loggerDef.getInstance().info("addPurchasePolicyRule: rule added to company " + companyId + " by " + userId);
            return true;
        } catch (Exception e) {
            loggerDef.getInstance().error("addPurchasePolicyRule failed: " + e.getMessage());
            return false;
        }
    }
}
