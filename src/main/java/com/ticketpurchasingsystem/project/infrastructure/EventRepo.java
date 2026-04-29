package com.ticketpurchasingsystem.project.infrastructure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.ticketpurchasingsystem.project.domain.event.Event;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;

public class EventRepo implements IEventRepo {

    private final Map<Integer, Event> storage = new HashMap<>();

    private final AtomicInteger idGenerator =
            new AtomicInteger(1);

    private static EventRepo instance;

    public static EventRepo getInstance() {

        if (instance == null) {
            instance = new EventRepo();
        }

        return instance;
    }
    @Override
    public Event save(Event event) {

        event.setEventId(idGenerator.getAndIncrement());

        storage.put(event.getEventId(), event);

        return event;
    }


    @Override
    public Event findById(Integer eventId) {
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
    public boolean delete(Integer eventId) {
        if (!storage.containsKey(eventId)) {
            return false;
        }
        storage.remove(eventId);
        return true;
    }
}