package com.ticketpurchasingsystem.project.acceptance.event;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ticketpurchasingsystem.project.domain.event.IEventRepo;
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
import com.ticketpurchasingsystem.project.domain.event.EventAggregatePublisher;
import com.ticketpurchasingsystem.project.infrastructure.EventRepo;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ViewEventDetailsAcceptanceTest {

    private EventService eventService;
    private String validToken;
    private String savedEventId;

    @Autowired
    private IEventRepo eventRepo;

    @BeforeEach
    void setUp() {
        eventRepo.deleteAll();

        // 1. Setup REAL Authentication
        InMemorySessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        // Exactly 32 characters long (256 bits)
        ReflectionTestUtils.setField(domainAuthService, "secret", "aSecureTestSecretKeyMustBe32Bytes");
        domainAuthService.init();
        AuthenticationService authService = new AuthenticationService(domainAuthService, sessionRepo);

        validToken = authService.login("adminUser");

        // 2. Simplified Anonymous Publisher
        ApplicationEventPublisher dummySpringPublisher = event -> {};
        EventAggregatePublisher simplePublisher = new EventAggregatePublisher(dummySpringPublisher);

        // 3. Setup REAL Service
        eventService = new EventService(eventRepo, simplePublisher, authService);

        // 4. Seed an event
        EventDTO newEvent = new EventDTO(
                null,
                42,
                "Cyberpunk Symphony",
                500,
                LocalDateTime.now().plusDays(10),
                true,
                "test location",
                null, // imageUrl
                null, // minZonePrice
                null  // maxZonePrice
        );
        PurchasePolicyDTO policy = new PurchasePolicyDTO(1, 10, false, null, null, false, false);

        eventService.createEvent(validToken, newEvent, policy, new ArrayList<>());

        // ✅ FIXED: Dynamically fetch the generated ID instead of hardcoding "1"
        List<EventDTO> companyEvents = eventService.searchEventsByCompany(validToken, 42);
        savedEventId = companyEvents.get(0).eventId();
    }

    @Test
    void GivenExistingEvent_WhenSearchEvent_ThenReturnCorrectEventDTO() {
        EventDTO resultDTO = eventService.searchEvent(validToken, savedEventId);

        assertNotNull(resultDTO);
        assertEquals("Cyberpunk Symphony", resultDTO.eventName());
        assertEquals(42, resultDTO.companyId());
        assertEquals(500, resultDTO.eventCapacity());
    }

    @Test
    void GivenNonExistentEventId_WhenSearchEvent_ThenReturnNull() {
        EventDTO resultDTO = eventService.searchEvent(validToken, "NON-EXISTENT-ID");
        assertNull(resultDTO);
    }

    @Test
    void GivenInvalidToken_WhenSearchEvent_ThenThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            eventService.searchEvent("bad-hacker-token", savedEventId);
        });
    }
}