package com.ticketpurchasingsystem.project.infrastructure.persistence;

import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public interface DBProdRepo extends JpaRepository<ProductionCompany, Integer>, IProdRepo {

    // Used by the findByName default method below
    Optional<ProductionCompany> findByCompanyNameIgnoreCase(String companyName);

    @Override
    default Optional<ProductionCompany> findByName(String name) {
        return findByCompanyNameIgnoreCase(name);
    }

    @Override
    @Query("SELECT DISTINCT p FROM ProductionCompany p LEFT JOIN p.members m " +
           "WHERE p.founderId = :userId OR (m.userId = :userId AND m.permission IS NULL " +
           "AND (m.status IS NULL OR m.status = " +
           "com.ticketpurchasingsystem.project.domain.Production.UserProductionCompany.MemberStatus.ACTIVE))")
    List<ProductionCompany> findAllByUserId(@Param("userId") String userId);

    @Override
    @Query("SELECT DISTINCT p FROM ProductionCompany p JOIN p.members m " +
           "WHERE m.userId = :userId AND m.permission IS NULL " +
           "AND m.status = com.ticketpurchasingsystem.project.domain.Production.UserProductionCompany.MemberStatus.PENDING")
    List<ProductionCompany> findAllWithPendingAppointee(@Param("userId") String userId);

}
