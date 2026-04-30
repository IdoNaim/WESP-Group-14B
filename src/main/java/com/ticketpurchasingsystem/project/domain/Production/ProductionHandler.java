package com.ticketpurchasingsystem.project.domain.Production;

import java.util.Optional;

import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.AssignOwnerEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.NewProdEvent;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

public class ProductionHandler {
    private final IProdRepo prodRepo;
    private final ProdPublisher publisher;

    public ProductionHandler(IProdRepo prodRepo, ProdPublisher publisher) {
        this.prodRepo = prodRepo;
        this.publisher = publisher;
    }

    public boolean createProductionCompany(String userId, ProductionCompanyDTO companyDetails) {
        if (userId == null || companyDetails == null || isInvalid(companyDetails.getCompanyName())) {
            return false;
        }
        Optional<ProductionCompany> existing = prodRepo.findByName(companyDetails.getCompanyName());
        if (existing.isPresent()) {
            loggerDef.getInstance().error("Company name already exists: " + companyDetails.getCompanyName());
            return false;
        }
        try {
            ProductionCompany newCompany = new ProductionCompany(companyDetails);
            newCompany.initFounder(userId);
            ProductionCompany savedCompany = prodRepo.save(newCompany);
            publisher.publish(new NewProdEvent(savedCompany));
            return true;
        } catch (Exception e) {
            loggerDef.getInstance().error("Failed to create company: " + e.getMessage());
            return false;
        }
    }

    public boolean assignOwner(String appointerId, Integer companyId, String appointeeUserId) {
        if (isInvalid(appointerId) || companyId == null || isInvalid(appointeeUserId)) {
            loggerDef.getInstance().error("assignOwner called with null/blank arguments");
            return false;
        }

        Optional<ProductionCompany> companyOpt = prodRepo.findById(companyId);
        if (companyOpt.isEmpty()) {
            loggerDef.getInstance().error("assignOwner: company not found, id=" + companyId);
            return false;
        }
        ProductionCompany company = companyOpt.get();

        if (!company.isOwner(appointerId)) {
            loggerDef.getInstance().error(
                    "assignOwner: caller " + appointerId + " is not an owner of company " + companyId);
            return false;
        }

        if (company.isOwner(appointeeUserId)) {
            loggerDef.getInstance().error(
                    "assignOwner: " + appointeeUserId + " is already an owner of company " + companyId);
            return false;
        }

        try {
            boolean appointed = company.appointOwner(appointerId, appointeeUserId);
            if (!appointed) {
                return false;
            }
            ProductionCompany saved = prodRepo.save(company);
            publisher.publish(new AssignOwnerEvent(saved, appointerId, appointeeUserId));
            loggerDef.getInstance().info(
                    "assignOwner: " + appointeeUserId + " appointed as owner of company "
                            + companyId + " by " + appointerId);
            return true;
        } catch (Exception e) {
            loggerDef.getInstance().error("assignOwner failed: " + e.getMessage());
            return false;
        }
    }

    private boolean isInvalid(String str) {
        return str == null || str.trim().isEmpty();
    }

}
