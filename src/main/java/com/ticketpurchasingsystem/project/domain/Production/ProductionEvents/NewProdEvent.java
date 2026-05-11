package com.ticketpurchasingsystem.project.domain.Production.ProductionEvents;

import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;

public class NewProdEvent {
    private final ProductionCompany company;

    public NewProdEvent(ProductionCompany company) {
        this.company = company;
    }

    public ProductionCompany getCompany() {
        return company;
    }
}
