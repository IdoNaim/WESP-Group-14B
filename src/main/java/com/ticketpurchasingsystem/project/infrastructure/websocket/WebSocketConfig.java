package com.ticketpurchasingsystem.project.infrastructure.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registers the presence channel at {@code /ws/presence}. The client opens it
 * right after login (passing {@code ?token=<session>}) and the server uses the
 * connection's lifetime to detect irregular exits — see {@link PresenceWebSocketHandler}.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final PresenceWebSocketHandler presenceWebSocketHandler;

    public WebSocketConfig(PresenceWebSocketHandler presenceWebSocketHandler) {
        this.presenceWebSocketHandler = presenceWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(presenceWebSocketHandler, "/ws/presence")
                // Dev origins (Vite on 5173/5174, CRA on 3000). Patterns avoid the
                // wildcard-with-credentials handshake rejection.
                .setAllowedOriginPatterns("*");
    }
}
