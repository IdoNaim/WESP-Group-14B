package com.ticketpurchasingsystem.project.Controllers;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ticketpurchasingsystem.project.Controllers.apidto.ConfigureSeatingMapRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.CreateEventRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.EditEventCapacityRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.EditEventDateRequestDTO;
import com.ticketpurchasingsystem.project.application.IEventService;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingAreaConfig;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingMap;
import com.ticketpurchasingsystem.project.domain.event.Maps.StandingAreaConfig;

@RestController
@RequestMapping("/api/events")
public class EventController {

        private final IEventService eventService;

        public EventController(IEventService eventService) {
                this.eventService = eventService;
        }

        // POST /api/events
        @PostMapping
        public ResponseEntity<Void> createEvent(
                @RequestHeader("Authorization") String authHeader,
                @RequestBody CreateEventRequestDTO body) {

                List<com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO> discounts = body
                        .getDiscounts() != null ? body.getDiscounts() : Collections.emptyList();

                // FIXED: Passed authHeader instead of body.getSessionToken() to centralize auth
                boolean success = eventService.createEvent(
                        authHeader,
                        body.getEvent(),
                        body.getPurchasePolicy(),
                        discounts
                );

                return success
                        ? ResponseEntity.status(HttpStatus.CREATED).build()
                        : ResponseEntity.badRequest().build();
        }

        // GET /api/events/{eventId}
        @GetMapping("/{eventId}")
        public ResponseEntity<EventDTO> getEvent(
                @RequestHeader("Authorization") String authHeader,
                @PathVariable String eventId) {
                String token = authHeader.startsWith("Bearer ") 
                ? authHeader.substring(7) 
                : authHeader;
                // FIXED: Added authHeader
                EventDTO result = eventService.searchEvent(token, eventId);
                return result != null
                        ? ResponseEntity.ok(result)
                        : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // GET /api/events?companyId=123
        @GetMapping
        public ResponseEntity<List<EventDTO>> getEventsByCompany(
                @RequestHeader("Authorization") String authHeader,
                @RequestParam int companyId) {

                // FIXED: Added authHeader
                List<EventDTO> events = eventService.searchEventsByCompany(authHeader, companyId);
                return ResponseEntity.ok(events != null ? events : Collections.emptyList());
        }

        // PUT /api/events/{eventId}/date
        @PutMapping("/{eventId}/date")
        public ResponseEntity<Void> editEventDate(
                @RequestHeader("Authorization") String authHeader,
                @PathVariable String eventId,
                @RequestBody EditEventDateRequestDTO body) {

                // FIXED: Added authHeader
                boolean success = eventService.editEventDate(authHeader, eventId, body.getNewDateTime());
                return success
                        ? ResponseEntity.ok().build()
                        : ResponseEntity.badRequest().build();
        }

        // PUT /api/events/{eventId}/capacity
        @PutMapping("/{eventId}/capacity")
        public ResponseEntity<Void> editEventCapacity(
                @RequestHeader("Authorization") String authHeader,
                @PathVariable String eventId,
                @RequestBody EditEventCapacityRequestDTO body) {

                // FIXED: Added authHeader
                boolean success = eventService.editEventInventory(authHeader, eventId, body.getNewCapacity());
                return success
                        ? ResponseEntity.ok().build()
                        : ResponseEntity.badRequest().build();
        }

        // DELETE /api/events/{eventId}
        @DeleteMapping("/{eventId}")
        public ResponseEntity<Void> removeEvent(
                @RequestHeader("Authorization") String authHeader,
                @PathVariable String eventId) {

                // FIXED: Added authHeader
                boolean success = eventService.removeEvent(authHeader, eventId);
                return success
                        ? ResponseEntity.ok().build()
                        : ResponseEntity.badRequest().build();
        }

        // PUT /api/events/{eventId}/seating-map
        @PutMapping("/{eventId}/seating-map")
        public ResponseEntity<Void> editSeatingMap(
                @RequestHeader("Authorization") String authHeader,
                @PathVariable String eventId,
                @RequestBody ConfigureSeatingMapRequestDTO body) {

                List<SeatingAreaConfig> seatingAreas = body.getSeatingAreas() == null
                        ? Collections.emptyList()
                        : body.getSeatingAreas().stream()
                        .map(a -> new SeatingAreaConfig(a.getRows(), a.getSeatsPerRow(),
                                a.getPrice()))
                        .collect(Collectors.toList());

                List<StandingAreaConfig> standingAreas = body.getStandingAreas() == null
                        ? Collections.emptyList()
                        : body.getStandingAreas().stream()
                        .map(a -> new StandingAreaConfig(a.getCapacity(), a.getPrice()))
                        .collect(Collectors.toList());

                // FIXED: Added authHeader to both configureSeatingMap and editEventSeatingMap
                SeatingMap seatingMap = eventService.configureSeatingMap(authHeader, seatingAreas, standingAreas);
                boolean success = eventService.editEventSeatingMap(authHeader, eventId, seatingMap);
                return success
                        ? ResponseEntity.ok().build()
                        : ResponseEntity.badRequest().build();
        }
        @GetMapping("/{eventId}/purchase-policy")
        public ResponseEntity<PurchasePolicyDTO> getEventPurchasePolicy(
                @RequestHeader("Authorization") String authHeader,
                @PathVariable String eventId) {
                String token = authHeader.startsWith("Bearer ") 
                ? authHeader.substring(7) 
                : authHeader;
                PurchasePolicyDTO purchasePolicy = eventService.getEventPurchasePolicy(token, eventId);
                return purchasePolicy != null
                        ? ResponseEntity.ok(purchasePolicy)
                        : ResponseEntity.notFound().build();
        }
        
}