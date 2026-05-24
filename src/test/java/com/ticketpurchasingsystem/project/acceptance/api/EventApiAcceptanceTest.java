package com.ticketpurchasingsystem.project.acceptance.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ticketpurchasingsystem.project.Controllers.EventController;
import com.ticketpurchasingsystem.project.Controllers.apidto.ConfigureSeatingMapRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.CreateEventRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.EditEventCapacityRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.EditEventDateRequestDTO;
import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.event.EventAggregateListener;
import com.ticketpurchasingsystem.project.domain.event.EventAggregatePublisher;
import com.ticketpurchasingsystem.project.infrastructure.EventRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Acceptance tests for the EventController HTTP layer.
 * Uses real services wired without Spring context — same pattern as existing
 * acceptance tests
 * but verified through HTTP requests instead of direct service calls.
 * ApplicationEventPublisher is mocked (no-op) since it is Spring
 * infrastructure, not business logic.
 */
class EventApiAcceptanceTest {

    private static final String VALID_AUTH = "Bearer some-token";

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        EventRepo eventRepo = new EventRepo();
        ApplicationEventPublisher noopPublisher = mock();
        EventAggregatePublisher eventPublisher = new EventAggregatePublisher(noopPublisher);
        EventAggregateListener eventListener = mock();
        eventService = new EventService(eventRepo, eventPublisher, eventListener);

        mockMvc = MockMvcBuilders.standaloneSetup(new EventController(eventService)).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // create event
    // POST /api/events
    @Test
    void GivenValidEvent_WhenCreateEvent_ThenReturn201() throws Exception {
        CreateEventRequestDTO dto = buildCreateEventRequest(1, "Summer Concert", 500);

        mockMvc.perform(post("/api/events")
                .header("Authorization", VALID_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void GivenInvalidPolicy_WhenCreateEvent_ThenReturn400() throws Exception {
        // minTickets > maxTickets violates the policy validation rule
        CreateEventRequestDTO dto = new CreateEventRequestDTO();
        dto.setEvent(new EventDTO(1, "Bad Policy Show", 200, LocalDateTime.now().plusDays(30), true));
        dto.setPurchasePolicy(new PurchasePolicyDTO(10, 1, 0, 120, false)); // minTickets=10 > maxTickets=1
        dto.setDiscounts(Collections.emptyList());

        mockMvc.perform(post("/api/events")
                .header("Authorization", VALID_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // get event
    // GET /api/events/{eventId}
    @Test
    void GivenCreatedEvent_WhenGetEvent_ThenReturn200WithCorrectData() throws Exception {
        String eventId = createEventAndGetId("Rock Night", 1, 300);

        mockMvc.perform(get("/api/events/" + eventId)
                .header("Authorization", VALID_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventName").value("Rock Night"))
                .andExpect(jsonPath("$.companyId").value(1));
    }

    @Test
    void GivenUnknownEventId_WhenGetEvent_ThenReturn404() throws Exception {
        mockMvc.perform(get("/api/events/nonexistent-id")
                .header("Authorization", VALID_AUTH))
                .andExpect(status().isNotFound());
    }

    // get events
    // GET /api/events?companyId=1
    @Test
    void GivenEventsForCompany_WhenGetEventsByCompany_ThenReturn200WithList() throws Exception {
        createEventAndGetId("Event A", 7, 100);
        createEventAndGetId("Event B", 7, 200);

        mockMvc.perform(get("/api/events")
                .param("companyId", "7")
                .header("Authorization", VALID_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void GivenNoEventsForCompany_WhenGetEventsByCompany_ThenReturn200WithEmptyList() throws Exception {
        mockMvc.perform(get("/api/events")
                .param("companyId", "999")
                .header("Authorization", VALID_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // edit date
    // PUT /api/events/{eventId}/date
    @Test
    void GivenCreatedEvent_WhenEditDate_ThenReturn200() throws Exception {
        String eventId = createEventAndGetId("Date Event", 2, 100);

        EditEventDateRequestDTO dto = new EditEventDateRequestDTO();
        dto.setNewDateTime(LocalDateTime.now().plusDays(60));

        mockMvc.perform(put("/api/events/" + eventId + "/date")
                .header("Authorization", VALID_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void GivenUnknownEvent_WhenEditDate_ThenReturn400() throws Exception {
        EditEventDateRequestDTO dto = new EditEventDateRequestDTO();
        dto.setNewDateTime(LocalDateTime.now().plusDays(30));

        mockMvc.perform(put("/api/events/nonexistent/date")
                .header("Authorization", VALID_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // edit capacity
    // PUT /api/events/{eventId}/capacity
    @Test
    void GivenCreatedEvent_WhenEditCapacity_ThenReturn200() throws Exception {
        String eventId = createEventAndGetId("Capacity Event", 3, 100);

        EditEventCapacityRequestDTO dto = new EditEventCapacityRequestDTO();
        dto.setNewCapacity(500);

        mockMvc.perform(put("/api/events/" + eventId + "/capacity")
                .header("Authorization", VALID_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void GivenUnknownEvent_WhenEditCapacity_ThenReturn400() throws Exception {
        EditEventCapacityRequestDTO dto = new EditEventCapacityRequestDTO();
        dto.setNewCapacity(200);

        mockMvc.perform(put("/api/events/nonexistent/capacity")
                .header("Authorization", VALID_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // remove event
    // DELETE /api/events/{eventId}
    @Test
    void GivenCreatedEvent_WhenRemoveEvent_ThenReturn200() throws Exception {
        String eventId = createEventAndGetId("Remove Me", 4, 50);

        mockMvc.perform(delete("/api/events/" + eventId)
                .header("Authorization", VALID_AUTH))
                .andExpect(status().isOk());
    }

    @Test
    void GivenUnknownEvent_WhenRemoveEvent_ThenReturn400() throws Exception {
        mockMvc.perform(delete("/api/events/nonexistent")
                .header("Authorization", VALID_AUTH))
                .andExpect(status().isBadRequest());
    }

    // configure seating map
    // PUT /api/events/{eventId}/seating-map
    @Test
    void GivenCreatedEvent_WhenConfigureSeatingMap_ThenReturn200() throws Exception {
        String eventId = createEventAndGetId("Seated Show", 5, 400);

        ConfigureSeatingMapRequestDTO dto = new ConfigureSeatingMapRequestDTO();
        ConfigureSeatingMapRequestDTO.SeatingAreaDTO area = new ConfigureSeatingMapRequestDTO.SeatingAreaDTO();
        area.setRows(10);
        area.setSeatsPerRow(20);
        area.setPrice(75.0);
        dto.setSeatingAreas(List.of(area));
        dto.setStandingAreas(Collections.emptyList());

        mockMvc.perform(put("/api/events/" + eventId + "/seating-map")
                .header("Authorization", VALID_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    // helpers
    private CreateEventRequestDTO buildCreateEventRequest(int companyId, String name, int capacity) {
        CreateEventRequestDTO dto = new CreateEventRequestDTO();
        dto.setEvent(new EventDTO(companyId, name, capacity, LocalDateTime.now().plusDays(30), true));
        dto.setPurchasePolicy(new PurchasePolicyDTO(1, 10, 0, 120, false));
        dto.setDiscounts(Collections.emptyList());
        return dto;
    }

    @SuppressWarnings("unchecked")
    private String createEventAndGetId(String name, int companyId, int capacity) throws Exception {
        CreateEventRequestDTO dto = buildCreateEventRequest(companyId, name, capacity);
        mockMvc.perform(post("/api/events")
                .header("Authorization", VALID_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)));

        // fetch from service directly to get the generated ID
        return eventService.searchEventsByCompany(companyId).stream()
                .filter(e -> e.eventName().equals(name))
                .findFirst()
                .map(e -> {
                    // get the id of the event from repository
                    for (int id = 1; id <= 100; id++) {
                        var found = eventService.searchEvent(String.valueOf(id));
                        if (found != null && name.equals(found.eventName()) && companyId == found.companyId()) {
                            return String.valueOf(id);
                        }
                    }
                    return null;
                })
                .orElseThrow(() -> new IllegalStateException("Event not found after creation: " + name));
    }
}
