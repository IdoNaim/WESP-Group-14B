package com.ticketpurchasingsystem.project.domain.Events;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.event.Event;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;
import com.ticketpurchasingsystem.project.domain.event.SeatingMap;

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
    void createEvent_shouldReturnTrue_whenValidInput() {

        EventDTO dto = new EventDTO(
                1,
                "Concert",
                100,
                LocalDateTime.now().plusDays(1),
                true
        );

        PurchasePolicyDTO policyDTO = mock(PurchasePolicyDTO.class);
        when(policyDTO.minTickets()).thenReturn(1);
        when(policyDTO.maxTickets()).thenReturn(10);
        when(policyDTO.minAge()).thenReturn(18);
        when(policyDTO.maxAge()).thenReturn(60);
        when(policyDTO.emnptySeatLeft()).thenReturn(false);

        List<DiscountDTO> discounts = Collections.emptyList();

        boolean result = eventService.createEvent(dto, policyDTO, discounts);

        assertTrue(result);
        verify(mockRepo).save(any(Event.class));
    }

    @Test
    void createEvent_shouldReturnFalse_whenRepoThrowsException() {

        EventDTO dto = new EventDTO(
                1,
                "Concert",
                100,
                LocalDateTime.now().plusDays(1),
                true
        );

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
    void searchEvent_shouldThrowException_whenNotImplemented() {
        assertThrows(UnsupportedOperationException.class,
                () -> eventService.searchEvent(1));
    }

    // ================= SEARCH EVENTS BY COMPANY =================

    @Test
    void searchEventsByCompany_shouldThrowException_whenNotImplemented() {
        assertThrows(UnsupportedOperationException.class,
                () -> eventService.searchEventsByCompany(1));
    }

    // ================= EDIT EVENT DATE =================

    @Test
    void editEventDate_shouldThrowException_whenNotImplemented() {
        assertThrows(UnsupportedOperationException.class,
                () -> eventService.editEventDate(1, LocalDateTime.now()));
    }

    // ================= REMOVE EVENT =================

    @Test
    void removeEvent_shouldDeleteEvent_whenEventExists() {

        Event mockEvent = mock(Event.class);
        when(mockRepo.findById(1)).thenReturn(Optional.of(mockEvent));

        boolean result = eventService.removeEvent(1);

        assertTrue(result);
        verify(mockRepo).delete(1);
    }

    @Test
    void removeEvent_shouldReturnFalse_whenEventNotFound() {

        when(mockRepo.findById(1)).thenReturn(Optional.empty());

        boolean result = eventService.removeEvent(1);

        assertFalse(result);
        verify(mockRepo, never()).delete(any());
    }

    // ================= EDIT INVENTORY =================

    @Test
    void editEventInventory_shouldThrowException_whenNotImplemented() {
        assertThrows(UnsupportedOperationException.class,
                () -> eventService.editEventInventory(1, 200));
    }

    // ================= CONFIGURE SEATING MAP =================

    @Test
    void configureEventSeatingMap_shouldThrowException_whenNotImplemented() {
        SeatingMap seatingMap = mock(SeatingMap.class);

        assertThrows(UnsupportedOperationException.class,
                () -> eventService.configureEventSeatinMap(1, seatingMap));
    }
}