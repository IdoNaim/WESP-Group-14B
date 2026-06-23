package com.ticketpurchasingsystem.project.domain.event;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;

public interface IEventRepo {

    Event save(Event event);

    Event findById(String eventId);

    List<Event> findByCompanyId(int companyId);

    List<Event> findActiveEvents();

    void delete(String eventId);

    void deleteAll();
}