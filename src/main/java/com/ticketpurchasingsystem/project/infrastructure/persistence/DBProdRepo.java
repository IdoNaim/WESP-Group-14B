package com.ticketpurchasingsystem.project.infrastructure.persistence;

import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.OptimisticLockingFailureException;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Primary
public class DBProdRepo implements IProdRepo {

    private final ProductionCompanyJpaRepository jpaRepo;

    @Autowired
    public DBProdRepo(ProductionCompanyJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public ProductionCompany save(ProductionCompany company) {
        try {
            ProductionCompany saved = jpaRepo.save(company);
            return new ProductionCompany(saved);
        } catch (org.springframework.dao.OptimisticLockingFailureException e) {
            throw new OptimisticLockingFailureException(
                    "Company " + company.getCompanyId() + " was modified concurrently: " + e.getMessage());
        }
    }

    @Override
    public Optional<ProductionCompany> findByName(String name) {
        return jpaRepo.findByCompanyNameIgnoreCase(name)
                .map(ProductionCompany::new);
    }

    @Override
    public Optional<ProductionCompany> findById(Integer companyId) {
        return jpaRepo.findById(companyId)
                .map(ProductionCompany::new);
    }

    @Override
    public List<ProductionCompany> findAllByUserId(String userId) {
        return jpaRepo.findAll().stream()
                .filter(c -> userId.equals(c.getFounderId())
                        || c.isOwner(userId)
                        || c.isManager(userId))
                .map(ProductionCompany::new)
                .collect(Collectors.toList());
    }
}
