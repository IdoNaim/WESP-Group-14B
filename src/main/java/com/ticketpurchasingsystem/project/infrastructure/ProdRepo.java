package com.ticketpurchasingsystem.project.infrastructure;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.OptimisticLockingFailureException;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;

public class ProdRepo implements IProdRepo {

    private final ConcurrentHashMap<Integer, ProductionCompany> storage = new ConcurrentHashMap<>();
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
            company.setVersion(0);
            storage.put(company.getCompanyId(), company);
            return new ProductionCompany(company);
        }

        ProductionCompany currentStored = storage.get(company.getCompanyId());
        if (currentStored == null) {
            throw new IllegalArgumentException("Company not found for update: " + company.getCompanyId());
        }

        if (company.getVersion() != currentStored.getVersion()) {
            throw new OptimisticLockingFailureException(
                    "Company " + company.getCompanyId()
                    + ": version mismatch. Expected " + company.getVersion()
                    + " but found " + currentStored.getVersion() + " in store.");
        }

        ProductionCompany updated = new ProductionCompany(company);
        updated.setVersion(company.getVersion() + 1);

        boolean replaced = storage.replace(company.getCompanyId(), currentStored, updated);
        if (!replaced) {
            throw new OptimisticLockingFailureException(
                    "Company " + company.getCompanyId()
                    + " was modified concurrently. Expected version " + company.getVersion() + ".");
        }

        return new ProductionCompany(updated);
    }

    @Override
    public Optional<ProductionCompany> findByName(String name) {
        return storage.values().stream()
                .filter(company -> company.getCompanyName().equalsIgnoreCase(name))
                .map(ProductionCompany::new)
                .findFirst();
    }

    @Override
    public Optional<ProductionCompany> findById(Integer companyId) {
        ProductionCompany stored = storage.get(companyId);
        return stored != null ? Optional.of(new ProductionCompany(stored)) : Optional.empty();
    }
}
