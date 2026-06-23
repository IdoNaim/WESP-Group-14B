package com.ticketpurchasingsystem.project.infrastructure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ticketpurchasingsystem.project.domain.event.Event;

/**
 * Verifies how a soft-cancelled event (isActive == false) behaves in the repository:
 * it stays findable and visible to the owning company, but is excluded from the
 * public "active events" listing.
 */
public class EventRepoCancelTest {

    private EventRepo repo;

    @BeforeEach
    void setUp() {
        repo = new EventRepo();
    }

    private Event saveCancelledEvent(int companyId, String name) {
        Event event = new Event(companyId, name, 100, LocalDateTime.now().plusDays(1), null, null, 0);
        Event saved = repo.save(event); // assigns id, version 0
        saved.cancel();
        return repo.save(saved); // soft-cancel persisted
    }

    @Test
    void GivenCancelledEvent_WhenFindById_ThenStillReturned() {
        Event cancelled = saveCancelledEvent(1, "Concert");

        Event found = repo.findById(cancelled.getEventId());

        assertNotNull(found, "A cancelled event must remain in the DB so order history can resolve its name");
        assertFalse(found.isActive(), "A cancelled event must have isActive == false");
    }

    @Test
    void GivenCancelledEvent_WhenFindActiveEvents_ThenExcluded() {
        Event cancelled = saveCancelledEvent(1, "Concert");

        boolean present = repo.findActiveEvents().stream()
                .anyMatch(e -> e.getEventId().equals(cancelled.getEventId()));

        assertFalse(present, "Cancelled events must not appear in the public active-events listing");
    }

    @Test
    void GivenCancelledEvent_WhenFindByCompanyId_ThenIncluded() {
        Event cancelled = saveCancelledEvent(1, "Concert");

        boolean present = repo.findByCompanyId(1).stream()
                .anyMatch(e -> e.getEventId().equals(cancelled.getEventId()));

        assertTrue(present, "Cancelled events must still appear on the company management page");
    }

    @Test
    void GivenActiveEvent_WhenFindActiveEvents_ThenIncluded() {
        Event event = new Event(2, "Live Show", 50, LocalDateTime.now().plusDays(2), null, null, 0);
        Event saved = repo.save(event);

        boolean present = repo.findActiveEvents().stream()
                .anyMatch(e -> e.getEventId().equals(saved.getEventId()));

        assertTrue(present, "Active events must appear in the public active-events listing");
    }
}
