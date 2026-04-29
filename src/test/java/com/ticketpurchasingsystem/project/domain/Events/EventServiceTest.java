package com.ticketpurchasingsystem.project.domain.Events;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

        // ================= EDIT EVENT DATE =================
    @Test
    void editEventDate_shouldReturnTrue_whenEventExists() {

        Event mockEvent = mock(Event.class);
        when(mockRepo.findById(1)).thenReturn(mockEvent);

        LocalDateTime newDate = LocalDateTime.now().plusDays(5);

        boolean result = eventService.editEventDate(1, newDate);

        assertTrue(result);
        verify(mockEvent).setEventDate(newDate);
        verify(mockRepo).save(mockEvent);
    }

    @Test
    void editEventDate_shouldReturnFalse_whenEventNotFound() {

        when(mockRepo.findById(1)).thenReturn(null);

        boolean result = eventService.editEventDate(1, LocalDateTime.now());

        assertFalse(result);
        verify(mockRepo, never()).save(any());
    }

    @Test
    void editEventDate_shouldReturnFalse_whenSaveThrowsException() {

        Event mockEvent = mock(Event.class);
        when(mockRepo.findById(1)).thenReturn(mockEvent);

        doThrow(new RuntimeException()).when(mockRepo).save(mockEvent);

        boolean result = eventService.editEventDate(1, LocalDateTime.now());

        assertFalse(result);
        verify(mockEvent).setEventDate(any());
    }

    // ================= REMOVE EVENT =================

    @Test
    void removeEvent_shouldDeleteEvent_whenEventExists() {

        when(mockRepo.delete(1)).thenReturn(true);

        boolean result = eventService.removeEvent(1);

        assertTrue(result);
        verify(mockRepo).delete(1);
    }

    @Test
    void removeEvent_shouldReturnFalse_whenEventNotFound() {

        when(mockRepo.delete(1)).thenReturn(false);

        boolean result = eventService.removeEvent(1);

        assertFalse(result);
        verify(mockRepo).delete(1);
    }
    // ================= EDIT INVENTORY =================

    // @Test
    // void editEventInventory_shouldThrowException_whenNotImplemented() {
    //     assertThrows(UnsupportedOperationException.class,
    //             () -> eventService.editEventInventory(1, 200));
    // }

    // // ================= CONFIGURE SEATING MAP =================

    // @Test
    // void configureEventSeatingMap_shouldThrowException_whenNotImplemented() {
    //     SeatingMap seatingMap = mock(SeatingMap.class);

    //     assertThrows(UnsupportedOperationException.class,
    //             () -> eventService.configureEventSeatinMap(1, seatingMap));
    // }
}