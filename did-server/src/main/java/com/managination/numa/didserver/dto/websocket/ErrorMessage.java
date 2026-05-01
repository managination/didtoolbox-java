package com.managination.numa.didserver.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorMessage(
    @JsonProperty("type") String type,
    @JsonProperty("sessionId") String sessionId,
    @JsonProperty("timestamp") String timestamp,
    @JsonProperty("code") String code,
    @JsonProperty("message") String message
) {
    public ErrorMessage(String sessionId, String code, String message) {
        this("error", sessionId, java.time.Instant.now().toString(), code, message);
    }
}
