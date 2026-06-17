package com.ticketpurchasingsystem.project.infrastructure.persistence;

import com.ticketpurchasingsystem.project.domain.event.Event;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@Primary
@Profile("!test")
public class DBEventRepo implements IEventRepo {

    private final EventJpaRepository eventJpaRepository;

    @Autowired
    public DBEventRepo(EventJpaRepository eventJpaRepository) {
        this.eventJpaRepository = eventJpaRepository;
    }

    @Override
    public Event save(Event event) {
        // Saves a new event or updates an existing one
        return eventJpaRepository.save(event);
    }

    @Override
    public Event findById(String eventId) {
        // Unwraps the Optional container for your domain layer
        return eventJpaRepository.findById(eventId).orElse(null);
    }

    @Override
    public List<Event> findByCompanyId(int companyId) {
        // Calls the custom query method we just added to the JPA repo
        return eventJpaRepository.findByCompanyId(companyId);
    }

    @Override
    public List<Event> findActiveEvents() {
        // Calls the custom active state query method
        return eventJpaRepository.findByIsActiveTrue();
    }

    @Override
    public void delete(String eventId) {
        // Deletes the record from the database by its primary key string
        eventJpaRepository.deleteById(eventId);
    }
}