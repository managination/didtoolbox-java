package com.managination.numa.didserver.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JoinMessage(
    @JsonProperty("type") String type,
    @JsonProperty("sessionId") String sessionId,
    @JsonProperty("timestamp") String timestamp,
    @JsonProperty("role") String role
) {
    public JoinMessage(String sessionId, String role) {
        this("join", sessionId, java.time.Instant.now().toString(), role);
    }
}
