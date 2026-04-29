package com.ticketpurchasingsystem.project.domain.event;

import java.util.List;

public interface IEventRepo {

    Event save(Event event);

    Optional<Event> findById(String eventId);

    List<Event> findByCompanyId(int companyId);

    List<Event> findActiveEvents();

    void delete(String eventId);
}