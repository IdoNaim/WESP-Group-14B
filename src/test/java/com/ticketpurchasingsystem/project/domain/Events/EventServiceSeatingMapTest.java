package com.ticketpurchasingsystem.project.domain.Events;

import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.domain.event.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EventServiceSeatingMapTest {

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(
                mock(IEventRepo.class),
                mock(EventAggregatePublisher.class),
                mock(EventAggregateListener.class)
        );
    }

    // ================= BASIC SUCCESS =================

    @Test
    void GivenValidConfigs_WhenConfigureSeatingMap_ThenMapContainsAreas() {

        SeatingAreaConfig seatingConfig = mock(SeatingAreaConfig.class);
        when(seatingConfig.getRows()).thenReturn(2);
        when(seatingConfig.getseatsPerRow()).thenReturn(3);
        when(seatingConfig.getPrice()).thenReturn(50.0);

        StandingAreaConfig standingConfig = mock(StandingAreaConfig.class);
        when(standingConfig.getCapacity()).thenReturn(100);
        when(standingConfig.getPrice()).thenReturn(20.0);

        SeatingMap map = eventService.configureSeatingMap(
                List.of(seatingConfig),
                List.of(standingConfig)
        );

        assertNotNull(map);

        // seating: 2 * 3 = 6 seats
        // standing: 1 standing area
        assertEquals(7, map.getPurchaseAreas().size());
    }

    // ================= EMPTY INPUT =================

    @Test
    void GivenEmptyLists_WhenConfigureSeatingMap_ThenReturnEmptyMap() {

        SeatingMap map = eventService.configureSeatingMap(
                List.of(),
                List.of()
        );

        assertNotNull(map);
        assertTrue(map.getPurchaseAreas().isEmpty());
    }

    // ================= INVALID SEATING =================

    @Test
    void GivenInvalidSeatingConfig_WhenConfigureSeatingMap_ThenNotAdded() {

        SeatingAreaConfig invalidConfig = mock(SeatingAreaConfig.class);

        when(invalidConfig.getRows()).thenReturn(0);
        when(invalidConfig.getseatsPerRow()).thenReturn(5);
        when(invalidConfig.getPrice()).thenReturn(50.0);

        SeatingMap map = eventService.configureSeatingMap(
                List.of(invalidConfig),
                List.of()
        );

        assertTrue(map.getPurchaseAreas().isEmpty());
    }

    // ================= INVALID STANDING =================

    @Test
    void GivenInvalidStandingConfig_WhenConfigureSeatingMap_ThenNotAdded() {

        StandingAreaConfig invalidStanding = mock(StandingAreaConfig.class);

        when(invalidStanding.getCapacity()).thenReturn(-10);
        when(invalidStanding.getPrice()).thenReturn(50.0);

        SeatingMap map = eventService.configureSeatingMap(
                List.of(),
                List.of(invalidStanding)
        );

        assertTrue(map.getPurchaseAreas().isEmpty());
    }

    // ================= MULTIPLE AREAS =================

    @Test
    void GivenMultipleConfigs_WhenConfigureSeatingMap_ThenAllAddedCorrectly() {

        SeatingAreaConfig seating1 = mock(SeatingAreaConfig.class);
        when(seating1.getRows()).thenReturn(2);
        when(seating1.getseatsPerRow()).thenReturn(2);
        when(seating1.getPrice()).thenReturn(30.0);

        SeatingAreaConfig seating2 = mock(SeatingAreaConfig.class);
        when(seating2.getRows()).thenReturn(1);
        when(seating2.getseatsPerRow()).thenReturn(4);
        when(seating2.getPrice()).thenReturn(40.0);

        StandingAreaConfig standing = mock(StandingAreaConfig.class);
        when(standing.getCapacity()).thenReturn(50);
        when(standing.getPrice()).thenReturn(10.0);

        SeatingMap map = eventService.configureSeatingMap(
                List.of(seating1, seating2),
                List.of(standing)
        );

        // seating1: 4 seats
        // seating2: 4 seats
        // standing: 1 area
        assertEquals(9, map.getPurchaseAreas().size());
    }
}