package com.ticketpurchasingsystem.project.infrastructure.persistence;

import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.IActiveOrderRepo;

import jakarta.persistence.LockModeType;

@Repository
@Primary
public interface DBActiveOrderRepo extends JpaRepository<ActiveOrderItem, String>, IActiveOrderRepo {

    // ── Spring Data derived queries ───────────────────────────────────────

    @Override
    ActiveOrderItem findByUserId(String userId);

    // ── Bridges for methods that don't align by name ──────────────────────

    @Override
    default void delete(String orderId) {
        deleteById(orderId);
    }

    @Override
    default void update(ActiveOrderItem order) {
        saveAndFlush(order);
    }

    // ── markAsProcessing with pessimistic row lock ────────────────────────

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM ActiveOrderItem o WHERE o.orderId = :orderId")
    Optional<ActiveOrderItem> findByIdForUpdate(@Param("orderId") String orderId);

    @Override
    @Transactional
    default boolean markAsProcessing(String orderId) {
        Optional<ActiveOrderItem> opt = findByIdForUpdate(orderId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Active order not found or already deleted: " + orderId);
        }
        ActiveOrderItem order = opt.get();
        boolean result = order.markAsProcessing();
        if (result) {
            saveAndFlush(order);
        }
        return result;
    }
}
