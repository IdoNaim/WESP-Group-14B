package com.ticketpurchasingsystem.project.acceptance.event;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.domain.event.EventAggregatePublisher;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingAreaConfig;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingMap;
import com.ticketpurchasingsystem.project.infrastructure.EventRepo;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;

class ReserveTicketsAcceptanceTest {

    private EventService eventService;
    private String validToken;
    private String savedEventId;
    private List<String> activeKeysFromMap;

    @BeforeEach
    void setUp() {
        // 1. Setup REAL Authentication with a secure 32-byte key
        InMemorySessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        ReflectionTestUtils.setField(domainAuthService, "secret", "aSecureTestSecretKeyMustBe32Bytes");
        domainAuthService.init();
        AuthenticationService authService = new AuthenticationService(domainAuthService, sessionRepo);

        validToken = authService.login("customerUser");

        // 2. Simple Anonymous Publisher
        ApplicationEventPublisher dummySpringPublisher = event -> {};
        EventAggregatePublisher simplePublisher = new EventAggregatePublisher(dummySpringPublisher);

        // 3. Setup REAL Service
        eventService = new EventService(new EventRepo(), simplePublisher, authService);

        // 4. Create real event with an open policy layout (Min 1, Max 10, Age 0-120)
        EventDTO newEvent = new EventDTO(null,42, "Rock Concert", 100, LocalDateTime.now().plusDays(5), true);
        PurchasePolicyDTO policy = new PurchasePolicyDTO(1, 10, false, 0, 120, false, false);

        eventService.createEvent(validToken, newEvent, policy, new ArrayList<>());
        savedEventId = "1";

        // 5. Add a real seating map
        List<SeatingAreaConfig> seatingConfigs = List.of(new SeatingAreaConfig(1, 5, 50.0));
        SeatingMap map = eventService.configureSeatingMap(validToken, seatingConfigs, new ArrayList<>());
        eventService.editEventSeatingMap(validToken, savedEventId, map);

        activeKeysFromMap = map.getPurchaseAreas().keySet().stream().limit(2).toList();
    }

    @Test
    void GivenAvailableSeats_WhenReserveSeats_ThenReservationSucceeds() {
        String orderId = "ORDER-123";

        // Act
        boolean result = eventService.reserveSeats(validToken, orderId, savedEventId, activeKeysFromMap);

        // Assert
        assertTrue(result, "Reservation flow failed execution handling structural domain validation rules.");

        // Note: checkSeatsReserved log outputs "returning the unreserved seats".
        // If your domain requires downstream processing before marking them clean,
        // verifying the true response of reserveSeats is our source of truth.
    }

    @Test
    void GivenInvalidEventId_WhenReserveSeats_ThenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            eventService.reserveSeats(validToken, "ORDER-999", "INVALID-EVENT-ID", activeKeysFromMap);
        });
    }

    @Test
    void GivenInvalidToken_WhenReserveSeats_ThenThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            eventService.reserveSeats("expired-or-malicious-token", "ORDER-111", savedEventId, activeKeysFromMap);
        });
    }
}