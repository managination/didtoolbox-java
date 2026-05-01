package com.managination.numa.didserver.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CredentialReceivedMessage(
    @JsonProperty("type") String type,
    @JsonProperty("sessionId") String sessionId,
    @JsonProperty("timestamp") String timestamp
) {
    public CredentialReceivedMessage(String sessionId) {
        this("credential_received", sessionId, java.time.Instant.now().toString());
    }
}
