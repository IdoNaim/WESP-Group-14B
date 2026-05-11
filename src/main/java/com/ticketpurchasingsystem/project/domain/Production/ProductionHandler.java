package com.ticketpurchasingsystem.project.domain.Production;

import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
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

    private boolean isInvalid(String str) {
        return str == null || str.trim().isEmpty();
    }

}
