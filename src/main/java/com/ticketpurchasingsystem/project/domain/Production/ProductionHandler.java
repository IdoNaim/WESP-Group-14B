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
        if (companyDetails == null || companyDetails.getCompanyName() == null
                || companyDetails.getCompanyName().trim().isEmpty()) {
            return false;
        }
        Optional<ProductionCompany> existingCompany = prodRepo.findByName(companyDetails.getCompanyName());
        if (existingCompany.isPresent()) {
            return false;
        }
        ProductionCompany company = new ProductionCompany(companyDetails);
        company.setFounderId(userId);
        company.addOwnerId(userId);
        try {
            ProductionCompany savedCompany = prodRepo.save(company);
            publisher.publish(new NewProdEvent(savedCompany));
            return true;
        } catch (Exception e) {
            loggerDef logger = loggerDef.getInstance();
            logger.error("Failed to create production company: " + companyDetails.getCompanyName());
            return false;
        }
    }
}
