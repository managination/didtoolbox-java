package com.managination.numa.didserver.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SessionExpiredMessage(
    @JsonProperty("type") String type,
    @JsonProperty("sessionId") String sessionId,
    @JsonProperty("timestamp") String timestamp,
    @JsonProperty("message") String message
) {
    public SessionExpiredMessage(String sessionId) {
        this("session_expired", sessionId, java.time.Instant.now().toString(), "Session has expired due to inactivity");
    }
}
