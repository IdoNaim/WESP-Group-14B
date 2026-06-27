package com.ticketpurchasingsystem.project.infrastructure;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.event.Event;
import com.ticketpurchasingsystem.project.domain.event.EventAggregatePublisher;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingAreaConfig;
import com.ticketpurchasingsystem.project.domain.event.Maps.StandingAreaConfig;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingMap;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class SeatingMapPersistenceTest {

    private EventService eventService;
    private String validToken;
    private String savedEventId;

    @Autowired
    private IEventRepo eventRepo;
    @Autowired
    private IHistoryOrderRepo historyOrderRepo;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        InMemorySessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        ReflectionTestUtils.setField(domainAuthService, "secret", "aSecureTestSecretKeyMustBe32Bytes");
        domainAuthService.init();
        AuthenticationService authService = new AuthenticationService(domainAuthService, sessionRepo);

        validToken = authService.login("adminUser");
        ApplicationEventPublisher dummySpringPublisher = event -> {};
        EventAggregatePublisher simplePublisher = new EventAggregatePublisher(dummySpringPublisher);

        eventService = new EventService(eventRepo, simplePublisher, authService, historyOrderRepo);

        // Create the base Event
        EventDTO newEvent = new EventDTO(null, 42, "DB Test Event", 500, LocalDateTime.now().plusDays(10), true, "test location", null, null, null);
        PurchasePolicyDTO policy = new PurchasePolicyDTO(1, 10, false, null, null, false, false);
        eventService.createEvent(validToken, newEvent, policy, new ArrayList<>());

        List<EventDTO> companyEvents = eventService.searchEventsByCompany(validToken, 42);
        savedEventId = companyEvents.get(0).eventId();
    }

    @Test
    void GivenEventWithMaps_WhenFlushedToDB_ThenAllRowsAreSaved() {
        // 1. Configure maps: 2 rows of 5 seats = 10 seats total. 1 standing area.
        List<SeatingAreaConfig> seatingConfigs = List.of(new SeatingAreaConfig(2, 5, 50.0));
        List<StandingAreaConfig> standingConfigs = List.of(new StandingAreaConfig(100, 30.0));

        SeatingMap map = eventService.configureSeatingMap(validToken, seatingConfigs, standingConfigs);
        eventService.editEventSeatingMap(validToken, savedEventId, map);

        // 2. FORCE DB INSERT & CLEAR CACHE (This is where the magic happens)
        entityManager.flush();
        entityManager.clear();

        // 3. Fetch from DB as if it's a completely new request
        Event retrievedEvent = eventRepo.findById(savedEventId);

        // 4. Verify DB actually saved the children
        assertNotNull(retrievedEvent, "Event should exist");
        assertNotNull(retrievedEvent.getSeatingMap(), "SeatingMap is null! Check CascadeType.ALL on Event.seatingMap");

        // Check exact counts to prove the rows were generated and saved
        assertEquals(10, retrievedEvent.getSeatingMap().getSeatIds().size(), "Expected exactly 10 seats saved in EVENTS_SEATS");
        assertEquals(1, retrievedEvent.getSeatingMap().getAreaIds().size(), "Expected exactly 1 area saved in EVENTS_STANDING_ARES");
    }
}