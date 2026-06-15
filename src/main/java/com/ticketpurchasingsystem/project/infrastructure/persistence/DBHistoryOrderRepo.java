package com.ticketpurchasingsystem.project.infrastructure.persistence;

import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;

@Repository
@Primary
public interface DBHistoryOrderRepo extends JpaRepository<ProductionCompany, Integer>, IHistoryOrderRepo {
    
    @Override
    @Query("SELECT * FROM HistoryOrderItem")
    List<HistoryOrderItem> findAll();

    @Override
    @Query("SELECT * FROM HistoryOrderItem h WHERE h.companyId = :companyId")
    List<HistoryOrderItem> findAllByCompanyId(@Param("companyId") int companyId);

    @Override
    @Query("SELECT * FROM HistoryOrderItem h WHERE h.userId = :userId")
    List<HistoryOrderItem> findAllByUserId(@Param("userId") String userId);

    @Override
    @Query("SELECT * FROM HistoryOrderItem h WHERE h.orderId = :orderId")
    HistoryOrderItem findByOrderId(@Param("orderId") String orderId);

    @Override
    @Query("SELECT * FROM HistoryOrderItem h WHERE h.eventId = :eventId")
    List<HistoryOrderItem> findAllByEventId(@Param("eventId") String eventId);
}
