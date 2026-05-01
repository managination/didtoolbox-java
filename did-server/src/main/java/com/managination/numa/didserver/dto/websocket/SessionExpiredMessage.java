package com.managination.numa.didserver.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Sent by the server to both parties when the session times out.")
public record SessionExpiredMessage(
    @Schema(description = "Message type identifier", example = "session_expired", requiredMode = REQUIRED)
    @JsonProperty("type") String type,

    @Schema(description = "The session this message belongs to", requiredMode = REQUIRED)
    @JsonProperty("sessionId") String sessionId,

    @Schema(description = "ISO-8601 timestamp of when the message was created")
    @JsonProperty("timestamp") String timestamp,

    @Schema(description = "Human-readable expiration notice", defaultValue = "Session has expired due to inactivity")
    @JsonProperty("message") String message
) {
    public SessionExpiredMessage(String sessionId) {
        this("session_expired", sessionId, java.time.Instant.now().toString(), "Session has expired due to inactivity");
    }
}
