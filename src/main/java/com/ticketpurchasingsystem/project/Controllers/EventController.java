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
import com.ticketpurchasingsystem.project.Controllers.apidto.ValidatePolicyRequestDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.SeatingMapDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.ConfigureSeatingMapRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.CreateEventRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.EditEventCapacityRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.EditEventDateRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.EditEventLocationRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.EditEventPriceRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.ValidatePolicyRequestDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.SeatingMapDTO;
import com.ticketpurchasingsystem.project.application.IEventService;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
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

                List<EventDTO> events = eventService.searchEventsByCompany(authHeader, companyId);
                return ResponseEntity.ok(events != null ? events : Collections.emptyList());
        }

        // PUT /api/events/{eventId}/date
        @PutMapping("/{eventId}/date")
        public ResponseEntity<Void> editEventDate(
                @RequestHeader("Authorization") String authHeader,
                @PathVariable String eventId,
                @RequestBody EditEventDateRequestDTO body) {

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

                boolean success = eventService.removeEvent(authHeader, eventId);
                return success
                        ? ResponseEntity.ok().build()
                        : ResponseEntity.badRequest().build();
        }

        // PUT /api/events/{eventId}/location
        @PutMapping("/{eventId}/location")
        public ResponseEntity<Void> editEventLocation(
                @RequestHeader("Authorization") String authHeader,
                @PathVariable String eventId,
                @RequestBody EditEventLocationRequestDTO body) {

                boolean success = eventService.editEventLocation(authHeader, eventId, body.getNewLocation());
                return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
        }

        // PUT /api/events/{eventId}/price
        @PutMapping("/{eventId}/price")
        public ResponseEntity<Void> editEventPrice(
                @RequestHeader("Authorization") String authHeader,
                @PathVariable String eventId,
                @RequestBody EditEventPriceRequestDTO body) {

                boolean success = eventService.editEventPrice(authHeader, eventId, body.getNewPrice());
                return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
        }

        // PUT /api/events/{eventId}/policy
        @PutMapping("/{eventId}/policy")
        public ResponseEntity<Void> editEventPolicy(
                @RequestHeader("Authorization") String authHeader,
                @PathVariable String eventId,
                @RequestBody PurchasePolicyDTO body) {

                boolean success = eventService.editEventPurchasePolicy(authHeader, eventId, body);
                return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
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
                        .map(a -> new SeatingAreaConfig(a.getRows(), a.getSeatsPerRow(), a.getPrice()))
                        .collect(Collectors.toList());

                List<StandingAreaConfig> standingAreas = body.getStandingAreas() == null
                        ? Collections.emptyList()
                        : body.getStandingAreas().stream()
                        .map(a -> new StandingAreaConfig(a.getCapacity(), a.getPrice()))
                        .collect(Collectors.toList());

                SeatingMap seatingMap = eventService.configureSeatingMap(authHeader, seatingAreas, standingAreas);
                boolean success = eventService.editEventSeatingMap(authHeader, eventId, seatingMap);
                return success
                        ? ResponseEntity.ok().build()
                        : ResponseEntity.badRequest().build();
        }
        @PostMapping("/{eventId}/validate-policy")
        public ResponseEntity<String> validatePurchasePolicy(
                @RequestHeader("Authorization") String authHeader,
                @PathVariable String eventId,
                @RequestBody ValidatePolicyRequestDTO body) {
                String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
                String violation = eventService.validatePurchasePolicy(token, eventId, body.getQuantity(), body.getUserAge());
                if (violation == null)
                        return ResponseEntity.ok().build();
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(violation);
        }
        
        @GetMapping("/{eventId}/seating-map")
        public ResponseEntity<SeatingMapDTO> getEventSeatingMap(
                @RequestHeader("Authorization") String authHeader,
                @PathVariable String eventId) {
                String token = authHeader.startsWith("Bearer ") 
                ? authHeader.substring(7) 
                : authHeader;
                SeatingMapDTO seatingMap = eventService.getEventSeatingMap(token, eventId);
                return seatingMap != null
                        ? ResponseEntity.ok(seatingMap)
                        : ResponseEntity.notFound().build();
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