package com.managination.numa.didserver.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SessionCancelledMessage(
    @JsonProperty("type") String type,
    @JsonProperty("sessionId") String sessionId,
    @JsonProperty("timestamp") String timestamp,
    @JsonProperty("message") String message
) {
    public SessionCancelledMessage(String sessionId) {
        this("session_cancelled", sessionId, java.time.Instant.now().toString(), "Session was cancelled by the issuer");
    }
}
