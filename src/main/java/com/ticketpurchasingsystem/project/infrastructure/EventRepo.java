package com.ticketpurchasingsystem.project.infrastructure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.ticketpurchasingsystem.project.domain.event.Event;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;

public class EventRepo implements IEventRepo {

    private final Map<String, Event> storage = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    @Override
    public Event save(Event event) {

        // Generate ID only if new
        if (event.getEventId() == null) {
            String id = String.valueOf(idGenerator.getAndIncrement());
            event.setEventId(id);
        }

        storage.put(event.getEventId(), event);
        return event;
    }

    @Override
    public Event findById(String eventId) {
        return storage.get(eventId);
    }

    @Override
    public List<Event> findByCompanyId(int companyId) {
        return storage.values()
                .stream()
                .filter(e -> e.getCompanyId() == companyId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Event> findActiveEvents() {
        return storage.values()
                .stream()
                .filter(Event::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String eventId) {
        storage.remove(eventId);
    }
}