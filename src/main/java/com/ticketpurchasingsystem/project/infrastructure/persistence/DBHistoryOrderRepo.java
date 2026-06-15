package com.ticketpurchasingsystem.project.infrastructure.persistence;

import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;

@Repository
@Primary
public interface DBHistoryOrderRepo extends JpaRepository<HistoryOrderItem, String>, IHistoryOrderRepo {
    
    @Override
    List<HistoryOrderItem> findAll();

    @Override
    List<HistoryOrderItem> findAllByCompanyId(int companyId);

    @Override
    List<HistoryOrderItem> findAllByUserId(String userId);

    @Override
    HistoryOrderItem findByOrderId(String orderId);

    @Override
    List<HistoryOrderItem> findAllByEventId(String eventId);
}
