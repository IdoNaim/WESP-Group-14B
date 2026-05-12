package com.ticketpurchasingsystem.project.domain.Production;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.ticketpurchasingsystem.project.domain.Utils.ManagerDTO;
import com.ticketpurchasingsystem.project.domain.Utils.OwnerDTO;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.RolesTreeDTO;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

import java.util.Set;

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
        boolean appointed = company.appointOwner(appointerId, appointeeUserId);
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

    public ProductionCompany modifyManagerPermissions(String ownerId, Integer companyId,
            String managerId, Set<ManagerPermission> permissions, ProductionCompany company) {
        if (isInvalid(ownerId) || companyId == null || isInvalid(managerId)
                || permissions == null || company == null) {
            loggerDef.getInstance().error("modifyManagerPermissions called with null/blank arguments");
            return null;
        }
        if (!company.isOwner(ownerId)) {
            loggerDef.getInstance().error(
                    "modifyManagerPermissions: caller " + ownerId + " is not an owner of company " + companyId);
            return null;
        }
        if (!company.isOwner(managerId)) {
            loggerDef.getInstance().error(
                    "modifyManagerPermissions: " + managerId + " is not a manager of company " + companyId);
            return null;
        }
        if (!company.isAppointedBy(managerId, ownerId)) {
            loggerDef.getInstance().error(
                    "modifyManagerPermissions: " + managerId + " was not appointed by " + ownerId);
            return null;
        }
        company.setManagerPermissions(managerId, permissions);
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
        if (company.isManager(managerId)) {
            loggerDef.getInstance().error(
                    "appointManager: " + managerId + " is already a manager of company " + companyId);
            return null;
        }
        boolean appointed = company.appointManager(appointerId, managerId, permissions);
        if (!appointed) {
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

        return new RolesTreeDTO(company.getCompanyId(), company.getFounderId(), ownershipTree, managerTree, managerPermissions);
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
    private boolean isInvalid(String str) {
        return str == null || str.trim().isEmpty();
    }

}
