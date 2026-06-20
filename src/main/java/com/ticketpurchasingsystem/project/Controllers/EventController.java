package com.ticketpurchasingsystem.project.Controllers;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
import com.ticketpurchasingsystem.project.Controllers.apidto.EditEventImageRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.EditEventLocationRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.EditEventPriceRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.ValidatePolicyRequestDTO;
import com.ticketpurchasingsystem.project.application.IEventService;
import com.ticketpurchasingsystem.project.application.IProductionService;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;
import com.ticketpurchasingsystem.project.domain.event.Event;
import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.MemberInfoDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.SeatingMapDTO;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingAreaConfig;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingMap;
import com.ticketpurchasingsystem.project.domain.event.Maps.StandingAreaConfig;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

@RestController
@RequestMapping("/api/events")
public class EventController {

        private final IEventService eventService;
        private final IProductionService productionService;
        private final IEventRepo eventRepo;

        public EventController(IEventService eventService, IProductionService productionService, IEventRepo eventRepo) {
                this.eventService = eventService;
                this.productionService = productionService;
                this.eventRepo = eventRepo;
        }

        private String extractToken(String authHeader) {
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        return authHeader.substring(7);
                }
                return authHeader;
        }

        private void checkPermission(String authHeader, Integer companyId, ManagerPermission requiredPerm) {
                String token = extractToken(authHeader);
                MemberInfoDTO memberInfo = productionService.getMyMemberInfo(token, companyId);
                if (memberInfo == null) {
                        throw new IllegalArgumentException("User is not a member of the production company or unauthorized.");
                }
                String role = memberInfo.getRole();
                if ("FOUNDER".equals(role) || "OWNER".equals(role)) {
                        return; // Fully authorized
                }
                if ("MANAGER".equals(role)) {
                        if (memberInfo.getPermissions() != null && memberInfo.getPermissions().contains(requiredPerm)) {
                                return; // Authorized manager
                        }
                }
                throw new IllegalArgumentException("Manager lacks the required permission: " + requiredPerm);
        }

        private void checkPermissionForEvent(String authHeader, String eventId, ManagerPermission requiredPerm) {
                Event event = eventRepo.findById(eventId);
                if (event == null) {
                        throw new IllegalArgumentException("Event not found with ID: " + eventId);
                }
                checkPermission(authHeader, event.getCompanyId(), requiredPerm);
        }

        // POST /api/events
        @PostMapping
        public ResponseEntity<String> createEvent(
                @RequestHeader("Authorization") String authHeader,
                @RequestBody CreateEventRequestDTO body) {

                if (body.getEvent() != null && body.getEvent().companyId() != null) {
                        checkPermission(authHeader, body.getEvent().companyId(), ManagerPermission.INVENTORY_MANAGEMENT);
                } else {
                        throw new IllegalArgumentException("Company ID is required to create an event.");
                }

                List<com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO> discounts = body
                        .getDiscounts() != null ? body.getDiscounts() : Collections.emptyList();

                String eventId = eventService.createEvent(
                        authHeader,
                        body.getEvent(),
                        body.getPurchasePolicy(),
                        discounts
                );

                return eventId != null
                        ? ResponseEntity.status(HttpStatus.CREATED).body(eventId)
                        : ResponseEntity.badRequest().build();
        }

        // GET /api/events/{eventId}
        @GetMapping("/{eventId}")
        public ResponseEntity<EventDTO> getEvent(
                @RequestHeader("Authorization") String authHeader,
                @PathVariable String eventId) {

                EventDTO result = eventService.searchEvent(authHeader, eventId);
                return result != null
                        ? ResponseEntity.ok(result)
                        : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // GET /api/events/active — public, no auth required
        @GetMapping("/active")
        public ResponseEntity<List<EventDTO>> getAllActiveEvents() {
                List<EventDTO> events = eventService.getAllActiveEvents();
                return ResponseEntity.ok(events != null ? events : Collections.emptyList());
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
                checkPermissionForEvent(authHeader, eventId, ManagerPermission.INVENTORY_MANAGEMENT);
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
                checkPermissionForEvent(authHeader, eventId, ManagerPermission.INVENTORY_MANAGEMENT);
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
                checkPermissionForEvent(authHeader, eventId, ManagerPermission.INVENTORY_MANAGEMENT);

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
                checkPermissionForEvent(authHeader, eventId, ManagerPermission.INVENTORY_MANAGEMENT);
                
                boolean success = eventService.editEventLocation(authHeader, eventId, body.getNewLocation());
                return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
        }

        // PUT /api/events/{eventId}/price
        @PutMapping("/{eventId}/price")
        public ResponseEntity<Void> editEventPrice(
                @RequestHeader("Authorization") String authHeader,
                @PathVariable String eventId,
                @RequestBody EditEventPriceRequestDTO body) {
                checkPermissionForEvent(authHeader, eventId, ManagerPermission.INVENTORY_MANAGEMENT);

                boolean success = eventService.editEventPrice(authHeader, eventId, body.getNewPrice());
                return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
        }

        // PUT /api/events/{eventId}/image
        @PutMapping("/{eventId}/image")
        public ResponseEntity<Void> editEventImage(
                @RequestHeader("Authorization") String authHeader,
                @PathVariable String eventId,
                @RequestBody EditEventImageRequestDTO body) {
                checkPermissionForEvent(authHeader, eventId, ManagerPermission.INVENTORY_MANAGEMENT);

                boolean success = eventService.editEventImage(authHeader, eventId, body.getNewImageUrl());
                return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
        }

        // PUT /api/events/{eventId}/policy
        @PutMapping("/{eventId}/policy")
        public ResponseEntity<Void> editEventPolicy(
                @RequestHeader("Authorization") String authHeader,
                @PathVariable String eventId,
                @RequestBody PurchasePolicyDTO body) {
                checkPermissionForEvent(authHeader, eventId, ManagerPermission.PURCHASING_AND_DISCOUNT_POLICY_MANAGEMENT);
                loggerDef.getInstance().info("Received request to edit purchase policy for event " + eventId);
                boolean success = eventService.editEventPurchasePolicy(authHeader, eventId, body);
                
                loggerDef.getInstance().info("Edit event policy for event " + eventId + ": " + (success ? "Success" : "Failure"));
                loggerDef.getInstance().info("New purchase policy: " + body);
                return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
        }

        // PUT /api/events/{eventId}/seating-map
        @PutMapping("/{eventId}/seating-map")
        public ResponseEntity<Void> editSeatingMap(
                @RequestHeader("Authorization") String authHeader,
                @PathVariable String eventId,
                @RequestBody ConfigureSeatingMapRequestDTO body) {
                checkPermissionForEvent(authHeader, eventId, ManagerPermission.VENUE_CONFIGURATION_AND_EVENT_MAPPING);

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
                loggerDef.getInstance().info("Retrieved purchase policy for event " + eventId + ": " + purchasePolicy);
                return purchasePolicy != null
                        ? ResponseEntity.ok(purchasePolicy)
                        : ResponseEntity.notFound().build();
        }
        // POST /api/events/{eventId}/validate-policy
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

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<java.util.Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        }
}