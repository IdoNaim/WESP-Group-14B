package com.ticketpurchasingsystem.project.domain.Production;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.Utils.ManagerDTO;
import com.ticketpurchasingsystem.project.domain.Utils.OwnerDTO;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.RolesTreeDTO;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

@Component
public class ProductionHandler {

    public ProductionCompany createProductionCompany(String userId, ProductionCompanyDTO companyDetails) {
        if (userId == null || companyDetails == null || isInvalid(companyDetails.getCompanyName())) {
            return null;
        }
        ProductionCompany newCompany = new ProductionCompany(companyDetails);
        newCompany.initFounder(userId);
        return newCompany;
    }

    public ProductionCompany assignOwner(String appointerId, Integer companyId,
            String appointeeUserId, ProductionCompany company) {
        if (isInvalid(appointerId) || companyId == null || isInvalid(appointeeUserId) || company == null) {
            loggerDef.getInstance().error("assignOwner called with null/blank arguments");
            return null;
        }
        if (!company.isOwner(appointerId)) {
            loggerDef.getInstance().error(
                    "assignOwner: caller " + appointerId + " is not an owner of company " + companyId);
            return null;
        }
        if (company.isOwner(appointeeUserId)) {
            loggerDef.getInstance().error(
                    "assignOwner: " + appointeeUserId + " is already an owner of company " + companyId);
            return null;
        }
        boolean appointed = company.requestOwner(appointerId, appointeeUserId);
        if (!appointed) {
            return null;
        }
        return company;
    }

    public boolean validateOwnerAccess(String userId, ProductionCompany company) {
        if (isInvalid(userId) || company == null) {
            loggerDef.getInstance().error("validateOwnerAccess: null or blank arguments");
            return false;
        }
        if (!company.isOwner(userId)) {
            loggerDef.getInstance().error(
                    "validateOwnerAccess: user " + userId + " is not an owner of company "
                            + company.getCompanyId());
            return false;
        }
        return true;
    }

    public boolean validateFounderAccess(String userId, ProductionCompany company) {
        if (isInvalid(userId) || company == null) {
            loggerDef.getInstance().error("validateFounderAccess: null or blank arguments");
            return false;
        }
        if (!company.isFounder(userId)) {
            loggerDef.getInstance().error(
                    "validateFounderAccess: user " + userId + " is not the founder of company "
                            + company.getCompanyId());
            return false;
        }
        return true;
    }

    public ProductionCompany modifyManagerPermissions(String requesterId, Integer companyId,
            String targetUserId, Set<ManagerPermission> permissions, ProductionCompany company) {
            
        if (isInvalid(requesterId) || companyId == null || isInvalid(targetUserId)
                || permissions == null || company == null) {
            loggerDef.getInstance().error("modifyManagerPermissions called with null/blank arguments");
            return null;
        }
        if (requesterId.equals(targetUserId)) {
            loggerDef.getInstance().error("modifyManagerPermissions: caller cannot modify their own permissions");
            return null;
        }
        boolean isFounder = company.isFounder(requesterId);
        boolean isOwner = company.isOwner(requesterId);
        
        if (!isFounder && !isOwner) {
            loggerDef.getInstance().error(
                    "modifyManagerPermissions: caller " + requesterId + " is not an owner or founder of company " + companyId);
            return null;
        }
        boolean targetIsOwner = company.isOwner(targetUserId);
        boolean targetIsManager = company.isManager(targetUserId);
        
        if (!targetIsOwner && !targetIsManager) {
            loggerDef.getInstance().error(
                    "modifyManagerPermissions: " + targetUserId + " is not a manager or owner of company " + companyId);
            return null;
        }
        if (!isFounder) {
            boolean hasHierarchy = false;
            if (targetIsOwner) {
                hasHierarchy = company.isAppointedBy(targetUserId, requesterId);
            } else if (targetIsManager) {
                hasHierarchy = company.isManagerAppointedByOwner(targetUserId, requesterId);
            }
            
            if (!hasHierarchy) {
                loggerDef.getInstance().error(
                        "modifyManagerPermissions: " + targetUserId + " was not appointed by " + requesterId);
                return null;
            }
        }
        boolean success = company.setManagerPermissions(targetUserId, permissions);
        if (!success) {
            return null;
        }
        
        return company;
    }
    public ProductionCompany appointManager(String appointerId, Integer companyId,
            String managerId, Set<ManagerPermission> permissions, ProductionCompany company) {
            
        if (isInvalid(appointerId) || companyId == null || isInvalid(managerId)
                || permissions == null || company == null) {
            loggerDef.getInstance().error("appointManager called with null/blank arguments");
            return null;
        }

        if (!company.isOwnerOrManager(appointerId)) {
            loggerDef.getInstance().error(
                    "appointManager: caller " + appointerId + " is not an owner or manager of company " + companyId);
            return null;
        }

        boolean requestCreated = company.requestManager(appointerId, managerId, permissions);
        
        if (!requestCreated) {
            loggerDef.getInstance().error(
                    "appointManager: " + managerId + " is already a manager or has a pending request in company " + companyId);
            return null;
        }
        
        return company;
    }
    public ProductionCompany acceptAppointment(String userId, ProductionCompany company) {
        if (isInvalid(userId) || company == null) {
            loggerDef.getInstance().error("acceptAppointment called with null/blank arguments");
            return null;
        }
        if (!company.hasPendingAppointment(userId)) {
            loggerDef.getInstance().error(
                    "acceptAppointment: no pending appointment for " + userId + " in company "
                            + company.getCompanyId());
            return null;
        }
        if (!company.acceptAppointment(userId)) {
            return null;
        }
        return company;
    }

    public ProductionCompany denyAppointment(String userId, ProductionCompany company) {
        if (isInvalid(userId) || company == null) {
            loggerDef.getInstance().error("denyAppointment called with null/blank arguments");
            return null;
        }
        if (!company.hasPendingAppointment(userId)) {
            loggerDef.getInstance().error(
                    "denyAppointment: no pending appointment for " + userId + " in company "
                            + company.getCompanyId());
            return null;
        }
        if (!company.denyAppointment(userId)) {
            return null;
        }
        return company;
    }

    public RolesTreeDTO getRolesTree(String userId, ProductionCompany company) {
        if (isInvalid(userId) || company == null) {
            loggerDef.getInstance().error("getRolesTree: null or blank arguments");
            return null;
        }
        if (!company.isOwner(userId) && !company.isFounder(userId)) {
            loggerDef.getInstance().error(
                    "getRolesTree: user " + userId + " is not an owner or founder of company "
                            + company.getCompanyId());
            return null;
        }

        Map<String, OwnerDTO> ownershipTree = new LinkedHashMap<>(company.getOwnershipTree());

        Map<String, ManagerDTO> managerTree = new LinkedHashMap<>(company.getManagerTree());

        Map<String, Set<ManagerPermission>> managerPermissions = new LinkedHashMap<>();
        for (String managerId : managerTree.keySet()) {
            managerPermissions.put(managerId, company.getManagerPermissions(managerId));
        }

        return new RolesTreeDTO(company.getCompanyId(), company.getCompanyName(), company.getFounderId(), ownershipTree, managerTree, managerPermissions);
    }


    public ProductionCompany removeOwner(String requesterId, Integer companyId, String ownerId, ProductionCompany company) {
        if (isInvalid(requesterId) || companyId == null || isInvalid(ownerId) || company == null) {
            loggerDef.getInstance().error("removeOwner called with null/blank arguments");
            return null;
        }
        if (!company.isOwner(requesterId) && !company.isFounder(requesterId)) {
            loggerDef.getInstance().error("removeOwner: caller " + requesterId + " is not an owner/founder of company " + companyId);
            return null;
        }
        boolean removed = company.removeOwner(requesterId, ownerId);
        if (!removed) {
            loggerDef.getInstance().error("removeOwner: failed to remove " + ownerId + " from company " + companyId);
            return null;
        }
        return company;
    }

    public ProductionCompany removeManager(String ownerId, Integer companyId, String managerId, ProductionCompany company){
        if (isInvalid(ownerId) || companyId == null || isInvalid(managerId) || company == null) {
            loggerDef.getInstance().error("removeManager called with null/blank arguments");
            return null;
        }
        if (!company.isOwner(ownerId)) {
            loggerDef.getInstance().error(
                    "modifyManagerPermissions: caller " + ownerId + " is not an owner of company " + companyId);
            return null;
        }
        if (!company.isManager(managerId)) {
            loggerDef.getInstance().error(
                    "appointManager: " + managerId + " is not a manager of company " + companyId);
            return null;
        }
        boolean appointed = company.removeManager(ownerId, managerId);
        if (!appointed) {
            return null;
        }
        return company;
    }
    public ProductionCompany addPurchasePolicyRule(String userId, Integer companyId, IPurchaseRule rule, ProductionCompany company) {
        if (isInvalid(userId) || companyId == null || rule == null || company == null) {
            loggerDef.getInstance().error("addPurchasePolicyRule called with null/blank arguments");
            return null;
        }
        if (!company.isOwner(userId)) {
            loggerDef.getInstance().error(
                    "addPurchasePolicyRule: caller " + userId + " is not an owner of company " + companyId);
            return null;
        }
        company.addPurchaseRule(rule);
        return company;
    }

    private boolean isInvalid(String str) {
        return str == null || str.trim().isEmpty();
    }

}
