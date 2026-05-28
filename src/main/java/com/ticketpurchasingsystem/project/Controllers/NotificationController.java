package com.ticketpurchasingsystem.project.Controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticketpurchasingsystem.project.Controllers.apidto.BroadcastNotificationRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.CreateNotificationRequestDTO;
import com.ticketpurchasingsystem.project.application.INotificationService;
import com.ticketpurchasingsystem.project.domain.Utils.NotificationDTO;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final INotificationService notificationService;

    public NotificationController(INotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // POST /api/notifications
    @PostMapping
    public ResponseEntity<?> createNotification(
            @RequestHeader("Authorization") String token,
            @RequestBody CreateNotificationRequestDTO body) {
        try {
            NotificationDTO created = notificationService.createNotification(token, body.getTargetUserId(), body.getMessage());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /api/notifications
    @GetMapping
    public ResponseEntity<?> getNotifications(
            @RequestHeader("Authorization") String token) {
        try {
            List<NotificationDTO> notifications = notificationService.getNotificationsForUser(token);
            return ResponseEntity.ok(notifications);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // GET /api/notifications/unread-count
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(
            @RequestHeader("Authorization") String token) {
        try {
            long count = notificationService.getUnreadCount(token);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // GET /api/notifications/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getNotificationById(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            NotificationDTO notification = notificationService.getNotificationById(token, id);
            return ResponseEntity.ok(notification);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg.contains("Access denied")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
        }
    }

    // POST /api/notifications/event/{eventId}
    @PostMapping("/event/{eventId}")
    public ResponseEntity<?> notifyEventAttendees(
            @RequestHeader("Authorization") String token,
            @PathVariable String eventId,
            @RequestBody BroadcastNotificationRequestDTO body) {
        try {
            List<NotificationDTO> created = notificationService.createNotificationsForEvent(token, eventId, body.getMessage());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // POST /api/notifications/production/{companyId}
    @PostMapping("/production/{companyId}")
    public ResponseEntity<?> notifyProductionMembers(
            @RequestHeader("Authorization") String token,
            @PathVariable int companyId,
            @RequestBody BroadcastNotificationRequestDTO body) {
        try {
            List<NotificationDTO> created = notificationService.createNotificationsForProduction(token, companyId, body.getMessage());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT /api/notifications/{id}/read
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            notificationService.markAsRead(token, id);
            return ResponseEntity.ok("Notification marked as read");
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg.contains("Access denied")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg);
            }
            return ResponseEntity.badRequest().body(msg);
        }
    }
}
