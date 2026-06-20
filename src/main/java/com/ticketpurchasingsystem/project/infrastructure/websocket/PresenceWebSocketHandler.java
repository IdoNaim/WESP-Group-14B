package com.ticketpurchasingsystem.project.infrastructure.websocket;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.domain.User.Events.UserEvents.UserLeavedPlatformEvent;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

/**
 * Presence channel: a logged-in client holds one WebSocket open for the lifetime
 * of its session. The connection itself is the liveness signal — when the user
 * closes the tab / loses the network, the socket closes and {@link #afterConnectionClosed}
 * fires. That is the "server gets no answer ⇒ user is gone" detection from the
 * Web Notifications lecture.
 *
 * <p>This is the only place that knows about WebSockets. On disconnect it merely
 * publishes a {@link UserLeavedPlatformEvent}; the application layer
 * (UserApplicationListener → UserService.handleDisconnect) decides what that means
 * for the DB, so domain/application stay free of WebSocket details.
 *
 * <p>Single-instance only: the session→user map lives in memory. After a restart the
 * map is empty, so the scheduled sweep (UserService.purgeExpiredSessions) is the backstop.
 */
@Component
public class PresenceWebSocketHandler extends TextWebSocketHandler {

    /** socketId → the session token that connection authenticated with. */
    private final Map<String, String> tokensBySocket = new ConcurrentHashMap<>();
    /** socketId → the userId behind the token. */
    private final Map<String, String> usersBySocket = new ConcurrentHashMap<>();

    private final AuthenticationService authenticationService;
    private final ApplicationEventPublisher eventPublisher;

    public PresenceWebSocketHandler(AuthenticationService authenticationService,
                                    ApplicationEventPublisher eventPublisher) {
        this.authenticationService = authenticationService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token = extractToken(session);
        if (token == null || !authenticationService.validate(token)) {
            // Unauthenticated / stale token: don't track it, just close.
            safeClose(session);
            return;
        }
        String userId = authenticationService.getUser(token);
        if (userId == null) {
            safeClose(session);
            return;
        }
        tokensBySocket.put(session.getId(), token);
        usersBySocket.put(session.getId(), userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String token = tokensBySocket.remove(session.getId());
        String userId = usersBySocket.remove(session.getId());
        if (userId == null || token == null) {
            return; // connection we never tracked (e.g. failed auth above)
        }
        // Hand off to the application layer; it ignores this if the user already
        // logged in again under a new token (race-safe by design).
        eventPublisher.publishEvent(new UserLeavedPlatformEvent(userId, token));
        loggerDef.getInstance().info("Presence socket closed for user " + userId + " (" + status + ")");
    }

    /** Pulls the {@code token} query parameter from the handshake URI. */
    private String extractToken(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        for (String pair : uri.getQuery().split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && "token".equals(pair.substring(0, eq))) {
                String value = pair.substring(eq + 1);
                return value.isEmpty() ? null : java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private void safeClose(WebSocketSession session) {
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (Exception ignored) {
            // best effort
        }
    }
}
