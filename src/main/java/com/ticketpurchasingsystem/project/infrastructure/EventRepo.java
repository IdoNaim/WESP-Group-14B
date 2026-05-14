package com.ticketpurchasingsystem.project.infrastructure;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.ticketpurchasingsystem.project.domain.Production.OptimisticLockingFailureException;
import com.ticketpurchasingsystem.project.domain.event.Event;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;

public class EventRepo implements IEventRepo {

    private final ConcurrentHashMap<String, Event> storage = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    // Thread-safe Singleton Implementation
    private static volatile EventRepo instance;

    public static EventRepo getInstance() {
        if (instance == null) {
            synchronized (EventRepo.class) {
                if (instance == null) {
                    instance = new EventRepo();
                }
            }
        }
        return instance;
    }

    @Override
    public Event save(Event event) {
        // 1. Create a brand new event
        if (event.getEventId() == null) {
            event.setEventId(String.valueOf(idGenerator.getAndIncrement()));
            event.setVersion(0);

            // Store a copy, return a copy
            storage.put(event.getEventId(), new Event(event));
            return new Event(event);
        }

        // 2. Update an existing event
        Event currentStored = storage.get(event.getEventId());
        if (currentStored == null) {
            throw new IllegalArgumentException("Event not found for update: " + event.getEventId());
        }

        // Optimistic Locking Check
        if (event.getVersion() != currentStored.getVersion()) {
            throw new OptimisticLockingFailureException(
                    "Event " + event.getEventId()
                            + ": version mismatch. Expected " + event.getVersion()
                            + " but found " + currentStored.getVersion() + " in store.");
        }

        // Increment version on a copy of the incoming event
        Event updated = new Event(event);
        updated.setVersion(event.getVersion() + 1);

        // Atomic replace
        boolean replaced = storage.replace(event.getEventId(), currentStored, updated);
        if (!replaced) {
            throw new OptimisticLockingFailureException(
                    "Event " + event.getEventId()
                            + " was modified concurrently. Expected version " + event.getVersion() + ".");
        }

        return new Event(updated);
    }

    @Override
    public Event findById(String eventId) {
        Event stored = storage.get(eventId);
        return stored != null ? new Event(stored) : null;
    }

    @Override
    public List<Event> findByCompanyId(int companyId) {
        return storage.values()
                .stream()
                .filter(e -> e.getCompanyId() == companyId)
                .map(Event::new) // Now works perfectly because of the copy constructor
                .collect(Collectors.toList());
    }

    @Override
    public List<Event> findActiveEvents() {
        return storage.values()
                .stream()
                .filter(Event::isActive)
                .map(Event::new) // Now works perfectly because of the copy constructor
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String eventId) {
        storage.remove(eventId);
    }
}