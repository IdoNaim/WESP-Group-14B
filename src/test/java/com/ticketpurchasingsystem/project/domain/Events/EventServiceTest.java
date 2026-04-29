package com.ticketpurchasingsystem.project.domain.Events;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.event.Event;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;

public class EventServiceTest {

    private EventService eventService;
    private IEventRepo mockRepo;

    @BeforeEach
    void setUp() {
        mockRepo = mock(IEventRepo.class);
        eventService = new EventService(mockRepo);
    }

    // ================= CREATE EVENT =================

    @Test
    void GivenValidInput_WhenCreateEvent_ThenReturnTrue() {

        EventDTO dto = new EventDTO(1, "Concert", 100,
                LocalDateTime.now().plusDays(1), true);

        PurchasePolicyDTO policyDTO = mock(PurchasePolicyDTO.class);
        when(policyDTO.minTickets()).thenReturn(1);
        when(policyDTO.maxTickets()).thenReturn(10);
        when(policyDTO.minAge()).thenReturn(18);
        when(policyDTO.maxAge()).thenReturn(60);
        when(policyDTO.emnptySeatLeft()).thenReturn(false);

        boolean result = eventService.createEvent(dto, policyDTO, Collections.emptyList());

        assertTrue(result);
        verify(mockRepo).save(any(Event.class));
    }

    @Test
    void GivenRepoFailure_WhenCreateEvent_ThenReturnFalse() {

        EventDTO dto = new EventDTO(1, "Concert", 100,
                LocalDateTime.now().plusDays(1), true);

        PurchasePolicyDTO policyDTO = mock(PurchasePolicyDTO.class);
        when(policyDTO.minTickets()).thenReturn(1);
        when(policyDTO.maxTickets()).thenReturn(10);
        when(policyDTO.minAge()).thenReturn(18);
        when(policyDTO.maxAge()).thenReturn(60);
        when(policyDTO.emnptySeatLeft()).thenReturn(false);

        doThrow(new RuntimeException()).when(mockRepo).save(any());

        boolean result = eventService.createEvent(dto, policyDTO, Collections.emptyList());

        assertFalse(result);
    }

    // ================= SEARCH EVENT =================

    @Test
    void GivenExistingEvent_WhenSearchEvent_ThenReturnDTO() {

        Event mockEvent = mock(Event.class);
        LocalDateTime now = LocalDateTime.now();

        when(mockEvent.getCompanyId()).thenReturn(1);
        when(mockEvent.getEventName()).thenReturn("Concert");
        when(mockEvent.getEventCapacity()).thenReturn(100);
        when(mockEvent.getEventDate()).thenReturn(now);
        when(mockEvent.isActive()).thenReturn(true);

        when(mockRepo.findById("1")).thenReturn(mockEvent);

        EventDTO result = eventService.searchEvent("1");

        assertNotNull(result);
        assertEquals("Concert", result.eventName());
        assertEquals(100, result.eventCapacity());
    }

    @Test
    void GivenNonExistingEvent_WhenSearchEvent_ThenReturnNull() {

        when(mockRepo.findById("1")).thenReturn(null);

        EventDTO result = eventService.searchEvent("1");

        assertNull(result);
    }

    // ================= SEARCH EVENTS BY COMPANY =================

    @Test
    void GivenEventsExist_WhenSearchEventsByCompany_ThenReturnList() {

        Event event = mock(Event.class);
        LocalDateTime now = LocalDateTime.now();

        when(event.getCompanyId()).thenReturn(1);
        when(event.getEventName()).thenReturn("Concert");
        when(event.getEventCapacity()).thenReturn(100);
        when(event.getEventDate()).thenReturn(now);
        when(event.isActive()).thenReturn(true);

        when(mockRepo.findByCompanyId(1)).thenReturn(List.of(event));

        List<EventDTO> result = eventService.searchEventsByCompany(1);

        assertEquals(1, result.size());
        assertEquals("Concert", result.get(0).eventName());
    }

    @Test
    void GivenNoEvents_WhenSearchEventsByCompany_ThenReturnEmptyList() {

        when(mockRepo.findByCompanyId(1)).thenReturn(Collections.emptyList());

        List<EventDTO> result = eventService.searchEventsByCompany(1);

        assertTrue(result.isEmpty());
    }

    // ================= EDIT EVENT DATE =================

    @Test
    void GivenExistingEvent_WhenEditEventDate_ThenUpdateAndReturnTrue() {

        Event mockEvent = mock(Event.class);
        when(mockRepo.findById("1")).thenReturn(mockEvent);

        LocalDateTime newDate = LocalDateTime.now().plusDays(5);

        boolean result = eventService.editEventDate("1", newDate);

        assertTrue(result);
        verify(mockEvent).setEventDate(newDate);
        verify(mockRepo).save(mockEvent);
    }

    @Test
    void GivenNonExistingEvent_WhenEditEventDate_ThenReturnFalse() {

        when(mockRepo.findById("1")).thenReturn(null);

        boolean result = eventService.editEventDate("1", LocalDateTime.now());

        assertFalse(result);
        verify(mockRepo, never()).save(any());
    }

    // ================= REMOVE EVENT =================

    @Test
    void GivenExistingEvent_WhenRemoveEvent_ThenDeleteAndReturnTrue() {

        Event mockEvent = mock(Event.class);
        when(mockRepo.findById("1")).thenReturn(mockEvent);

        boolean result = eventService.removeEvent("1");

        assertTrue(result);
        verify(mockRepo).delete("1");
    }

    @Test
    void GivenNonExistingEvent_WhenRemoveEvent_ThenReturnFalse() {

        when(mockRepo.findById("1")).thenReturn(null);

        boolean result = eventService.removeEvent("1");

        assertFalse(result);
        verify(mockRepo, never()).delete(any());
    }

    // ================= EDIT EVENT CAPACITY =================

    @Test
    void GivenExistingEvent_WhenEditEventInventory_ThenUpdateAndReturnTrue() {

        Event mockEvent = mock(Event.class);
        when(mockRepo.findById("1")).thenReturn(mockEvent);

        int newCapacity = 250;

        boolean result = eventService.editEventInventory("1", newCapacity);

        assertTrue(result);
        verify(mockEvent).setEventCapacity(newCapacity);
        verify(mockRepo).save(mockEvent);
    }

    @Test
    void GivenNonExistingEvent_WhenEditEventInventory_ThenReturnFalse() {

        when(mockRepo.findById("1")).thenReturn(null);

        boolean result = eventService.editEventInventory("1", 250);

        assertFalse(result);
        verify(mockRepo, never()).save(any());
    }
}