package com.ticketpurchasingsystem.project.Controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ticketpurchasingsystem.project.application.IEventService;

@RestController
@RequestMapping("/api/tickets")
public class TicketsController {

    private final IEventService eventService;

    public TicketsController(IEventService eventService) {
        this.eventService = eventService;
    }

    // GET /api/tickets/{eventId}/seats/availability?seatIds=zone_row_col,zone_row_col
    // Returns whether every requested seat is currently unbooked.
    @GetMapping("/{eventId}/seats/availability")
    public ResponseEntity<Map<String, Object>> checkSeatAvailability(
            @PathVariable String eventId,
            @RequestParam List<String> seatIds) {

        boolean available = eventService.checkSeatAvailability(eventId, seatIds);
        if (available) {
            return ResponseEntity.ok(Map.of("available", true));
        }
        return ResponseEntity.ok(Map.of(
                "available", false,
                "reason", "One or more seats are unavailable or do not exist"));
    }

    // GET /api/tickets/{eventId}/standing/availability?areaId=X&quantity=5
    // Returns whether the standing area has enough capacity for the requested quantity.
    @GetMapping("/{eventId}/standing/availability")
    public ResponseEntity<Map<String, Object>> checkStandingAreaAvailability(
            @PathVariable String eventId,
            @RequestParam String areaId,
            @RequestParam int quantity) {

        boolean available = eventService.checkStandingAreaAvailability(eventId, areaId, quantity);
        if (available) {
            return ResponseEntity.ok(Map.of("available", true));
        }
        return ResponseEntity.ok(Map.of(
                "available", false,
                "reason", "Insufficient capacity in standing area"));
    }
}
