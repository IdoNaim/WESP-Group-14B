package com.ticketpurchasingsystem.project.domain.event;

import java.util.List;

public interface IEventRepo {

    Event save(Event event);

    Event findById(Integer eventId);

    List<Event> findByCompanyId(int companyId);

    List<Event> findActiveEvents();

    boolean delete(Integer eventId);
}