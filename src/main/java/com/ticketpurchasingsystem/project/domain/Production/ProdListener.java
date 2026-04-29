package com.ticketpurchasingsystem.project.domain.Production;

import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.NewProdEvent;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

public class ProdListener {

    public void onNewProductionCompany(NewProdEvent event) {
        String companyName = event.getCompany().getCompanyName();
        String founderId = event.getCompany().getFounderId();
        loggerDef logger = loggerDef.getInstance();
        logger.info("New Production Company created: Name=" + companyName + ", FounderID=" + founderId);
    }
}