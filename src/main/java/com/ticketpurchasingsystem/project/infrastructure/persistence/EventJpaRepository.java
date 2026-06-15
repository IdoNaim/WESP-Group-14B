package com.ticketpurchasingsystem.project.infrastructure.persistence;

import com.ticketpurchasingsystem.project.domain.event.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EventJpaRepository extends JpaRepository<Event, String> {

    // Spring parses this method name to generate: SELECT * FROM events WHERE company_id = ?
    List<Event> findByCompanyId(int companyId);

    // Spring parses this method name to generate: SELECT * FROM events WHERE is_active = true
    List<Event> findByIsActiveTrue();
}