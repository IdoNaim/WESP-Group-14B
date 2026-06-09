package com.ticketpurchasingsystem.project.infrastructure.persistence;

import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProductionCompanyJpaRepository extends JpaRepository<ProductionCompany, Integer> {

    Optional<ProductionCompany> findByCompanyNameIgnoreCase(String companyName);
}
