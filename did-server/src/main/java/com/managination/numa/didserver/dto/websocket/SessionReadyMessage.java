package com.managination.numa.didserver.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SessionReadyMessage(
    @JsonProperty("type") String type,
    @JsonProperty("sessionId") String sessionId,
    @JsonProperty("timestamp") String timestamp
) {
    public SessionReadyMessage(String sessionId) {
        this("session_ready", sessionId, java.time.Instant.now().toString());
    }
}
