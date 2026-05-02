package com.ticketpurchasingsystem.project.domain.Production;

import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

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

    private boolean isInvalid(String str) {
        return str == null || str.trim().isEmpty();
    }

}
