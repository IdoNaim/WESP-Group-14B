package com.ticketpurchasingsystem.project.domain.Production;

import java.util.Optional;

public interface IProdRepo {
    ProductionCompany save(ProductionCompany company);
    Optional<ProductionCompany> findByName(String name);
}
