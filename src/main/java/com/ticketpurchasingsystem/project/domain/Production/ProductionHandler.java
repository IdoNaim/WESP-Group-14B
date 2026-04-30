package com.ticketpurchasingsystem.project.domain.Production;

import java.util.Optional;

import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
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
        // Basic input validation
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

    private boolean isInvalid(String str) {
        return str == null || str.trim().isEmpty();
    }

}
