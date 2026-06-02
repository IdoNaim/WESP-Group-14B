package com.ticketpurchasingsystem.project.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ticketpurchasingsystem.project.Controllers.apidto.ConfigureSeatingMapRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.CreateEventRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.EditEventCapacityRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.EditEventDateRequestDTO;
import com.ticketpurchasingsystem.project.application.IEventService;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@Import(SecurityConfig.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IEventService eventService;

    private ObjectMapper objectMapper;
    private static final String VALID_AUTH = "Bearer valid-token";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // ================= create event =================
    // POST /api/events

    @Test
    void GivenValidRequest_WhenCreateEvent_ThenReturn201() throws Exception {
        CreateEventRequestDTO dto = new CreateEventRequestDTO();
        dto.setEvent(new EventDTO(null,1, "Concert Night", 500, LocalDateTime.now().plusDays(7), true));
        dto.setPurchasePolicy(mock(PurchasePolicyDTO.class));

        // FIXED: Added 4th 'any()' for authHeader
        when(eventService.createEvent(any(), any(), any(), any())).thenReturn("evt-1");

        mockMvc.perform(post("/api/events")
                        .header("Authorization", VALID_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void GivenServiceFailure_WhenCreateEvent_ThenReturn400() throws Exception {
        CreateEventRequestDTO dto = new CreateEventRequestDTO();
        dto.setEvent(new EventDTO(null,1, "Concert Night", 500, LocalDateTime.now().plusDays(7), true));

        // FIXED: Added 4th 'any()' for authHeader
        when(eventService.createEvent(any(), any(), any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/events")
                        .header("Authorization", VALID_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ================= get event by id =================
    // GET /api/events/{eventId}

    @Test
    void GivenExistingEvent_WhenGetEvent_ThenReturn200WithBody() throws Exception {
        EventDTO event = new EventDTO("evt-1", 1, "Rock Festival", 1000, LocalDateTime.now().plusDays(14), true);

        // FIXED: Added eq(VALID_AUTH)
        when(eventService.searchEvent(eq(VALID_AUTH), eq("evt-1"))).thenReturn(event);

        mockMvc.perform(get("/api/events/evt-1")
                        .header("Authorization", VALID_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventName").value("Rock Festival"))
                .andExpect(jsonPath("$.companyId").value(1));
    }

    @Test
    void GivenUnknownEvent_WhenGetEvent_ThenReturn404() throws Exception {
        // FIXED: Added second any()
        when(eventService.searchEvent(any(), any())).thenReturn(null);

        mockMvc.perform(get("/api/events/unknown-id")
                        .header("Authorization", VALID_AUTH))
                .andExpect(status().isNotFound());
    }

    // ================= get all events by company id =================
    // GET /api/events?companyId=1

    @Test
    void GivenCompanyWithEvents_WhenGetEventsByCompany_ThenReturn200WithList() throws Exception {
        List<EventDTO> events = List.of(
                new EventDTO("evt-1",1, "Event A", 200, LocalDateTime.now().plusDays(5), true),
                new EventDTO("evt-2",1, "Event B", 300, LocalDateTime.now().plusDays(10), true));

        // FIXED: Added eq(VALID_AUTH)
        when(eventService.searchEventsByCompany(eq(VALID_AUTH), eq(1))).thenReturn(events);

        mockMvc.perform(get("/api/events")
                        .param("companyId", "1")
                        .header("Authorization", VALID_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void GivenCompanyWithNoEvents_WhenGetEventsByCompany_ThenReturn200WithEmptyList() throws Exception {
        // FIXED: Added any() for authHeader
        when(eventService.searchEventsByCompany(any(), eq(99))).thenReturn(null);

        mockMvc.perform(get("/api/events")
                        .param("companyId", "99")
                        .header("Authorization", VALID_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ================= edit event date =================
    // PUT /api/events/{eventId}/date

    @Test
    void GivenValidDate_WhenEditEventDate_ThenReturn200() throws Exception {
        EditEventDateRequestDTO dto = new EditEventDateRequestDTO();
        dto.setNewDateTime(LocalDateTime.now().plusDays(30));

        // FIXED: Added eq(VALID_AUTH)
        when(eventService.editEventDate(eq(VALID_AUTH), eq("evt-1"), any())).thenReturn(true);

        mockMvc.perform(put("/api/events/evt-1/date")
                        .header("Authorization", VALID_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void GivenUnknownEvent_WhenEditEventDate_ThenReturn400() throws Exception {
        EditEventDateRequestDTO dto = new EditEventDateRequestDTO();
        dto.setNewDateTime(LocalDateTime.now().plusDays(30));

        // FIXED: Added 3rd any() for authHeader
        when(eventService.editEventDate(any(), any(), any())).thenReturn(false);

        mockMvc.perform(put("/api/events/unknown/date")
                        .header("Authorization", VALID_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ================= edit event capacity =================
    // PUT /api/events/{eventId}/capacity

    @Test
    void GivenValidCapacity_WhenEditCapacity_ThenReturn200() throws Exception {
        EditEventCapacityRequestDTO dto = new EditEventCapacityRequestDTO();
        dto.setNewCapacity(750);

        // FIXED: Added eq(VALID_AUTH) and matcher for capacity
        when(eventService.editEventInventory(eq(VALID_AUTH), eq("evt-1"), eq(750))).thenReturn(true);

        mockMvc.perform(put("/api/events/evt-1/capacity")
                        .header("Authorization", VALID_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void GivenUnknownEvent_WhenEditCapacity_ThenReturn400() throws Exception {
        EditEventCapacityRequestDTO dto = new EditEventCapacityRequestDTO();
        dto.setNewCapacity(100);

        // FIXED: Added 3rd any() for authHeader
        when(eventService.editEventInventory(any(), any(), any(Integer.class))).thenReturn(false);

        mockMvc.perform(put("/api/events/unknown/capacity")
                        .header("Authorization", VALID_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ================= remove event =================
    // DELETE /api/events/{eventId}

    @Test
    void GivenExistingEvent_WhenRemoveEvent_ThenReturn200() throws Exception {
        // FIXED: Added eq(VALID_AUTH)
        when(eventService.removeEvent(eq(VALID_AUTH), eq("evt-1"))).thenReturn(true);

        mockMvc.perform(delete("/api/events/evt-1")
                        .header("Authorization", VALID_AUTH))
                .andExpect(status().isOk());
    }

    @Test
    void GivenUnknownEvent_WhenRemoveEvent_ThenReturn400() throws Exception {
        // FIXED: Added 2nd any() for authHeader
        when(eventService.removeEvent(any(), any())).thenReturn(false);

        mockMvc.perform(delete("/api/events/unknown")
                        .header("Authorization", VALID_AUTH))
                .andExpect(status().isBadRequest());
    }

    // ================= configure seating map =================
    // PUT /api/events/{eventId}/seating-map

    @Test
    void GivenValidSeatingMap_WhenConfigureSeatingMap_ThenReturn200() throws Exception {
        ConfigureSeatingMapRequestDTO dto = new ConfigureSeatingMapRequestDTO();
        ConfigureSeatingMapRequestDTO.SeatingAreaDTO area = new ConfigureSeatingMapRequestDTO.SeatingAreaDTO();
        area.setRows(10);
        area.setSeatsPerRow(20);
        area.setPrice(50.0);
        dto.setSeatingAreas(List.of(area));
        dto.setStandingAreas(Collections.emptyList());

        // FIXED: Added authHeader parameter to both mock setups
        when(eventService.configureSeatingMap(any(), any(), any())).thenReturn(mock(SeatingMap.class));
        when(eventService.editEventSeatingMap(eq(VALID_AUTH), eq("evt-1"), any())).thenReturn(true);

        mockMvc.perform(put("/api/events/evt-1/seating-map")
                        .header("Authorization", VALID_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void GivenUnknownEvent_WhenConfigureSeatingMap_ThenReturn400() throws Exception {
        ConfigureSeatingMapRequestDTO dto = new ConfigureSeatingMapRequestDTO();
        dto.setSeatingAreas(Collections.emptyList());
        dto.setStandingAreas(Collections.emptyList());

        // FIXED: Added authHeader parameter to both mock setups
        when(eventService.configureSeatingMap(any(), any(), any())).thenReturn(mock(SeatingMap.class));
        when(eventService.editEventSeatingMap(any(), any(), any())).thenReturn(false);

        mockMvc.perform(put("/api/events/unknown/seating-map")
                        .header("Authorization", VALID_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}