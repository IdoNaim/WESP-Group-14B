package com.ticketpurchasingsystem.project.domain.Production;

import java.util.List;
import java.util.Optional;

public interface IProdRepo {
    ProductionCompany save(ProductionCompany company);

    Optional<ProductionCompany> findByName(String name);

    Optional<ProductionCompany> findById(Integer companyId);

    List<ProductionCompany> findAllByUserId(String userId);

    void deleteAll();
}
