package com.ticketpurchasingsystem.project.infrastructure;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;

public class ProdRepo implements IProdRepo {

    private final Map<Integer, ProductionCompany> storage = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    private static ProdRepo instance;

    public static ProdRepo getInstance() {
        if (instance == null) {
            instance = new ProdRepo();
        }
        return instance;
    }

    @Override
    public ProductionCompany save(ProductionCompany company) {
        if (company.getCompanyId() == null) {
            company.setCompanyId(idGenerator.getAndIncrement());
        }
        storage.put(company.getCompanyId(), company);
        return company;
    }

    @Override
    public Optional<ProductionCompany> findByName(String name) {
        return storage.values().stream()
                .filter(company -> company.getCompanyName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public Optional<ProductionCompany> findById(Integer companyId) {
        return Optional.ofNullable(storage.get(companyId));
    }
}
