package com.ticketpurchasingsystem.project.infrastructure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.ticketpurchasingsystem.project.domain.event.Event;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;

public class EventRepo implements IEventRepo {

    private final Map<Integer, Event> storage = new HashMap<>();

    private final AtomicInteger idGenerator =
            new AtomicInteger(1);


    @Override
    public Event save(Event event) {

        if (event.getEventId() == null) {

            try {

                var field =
                        Event.class.getDeclaredField("eventId");

                field.setAccessible(true);

                field.set(event, idGenerator.getAndIncrement());

            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed assigning ID to event"
                );
            }
        }

        storage.put(event.getEventId(), event);

        return event;
    }


    @Override
    public Optional<Event> findById(Integer eventId) {

        return Optional.ofNullable(
                storage.get(eventId)
        );
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
    public void delete(Integer eventId) {

        storage.remove(eventId);
    }
}