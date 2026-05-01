package com.managination.numa.didserver.config;

import com.managination.numa.didserver.websocket.SessionWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SessionWebSocketHandler sessionWebSocketHandler;

    @Value("${websocket.allowed-origins:*}")
    private String allowedOrigins;

    public WebSocketConfig(SessionWebSocketHandler sessionWebSocketHandler) {
        this.sessionWebSocketHandler = sessionWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        registry.addHandler(sessionWebSocketHandler, "/ws/session/{sessionId}")
                .setAllowedOrigins(origins);
    }
}
