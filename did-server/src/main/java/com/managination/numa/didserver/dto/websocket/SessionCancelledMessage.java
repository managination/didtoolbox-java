package com.managination.numa.didserver.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Sent by the server to the holder when the issuer cancels the session.")
public record SessionCancelledMessage(
    @Schema(description = "Message type identifier", example = "session_cancelled", requiredMode = REQUIRED)
    @JsonProperty("type") String type,

    @Schema(description = "The session this message belongs to", requiredMode = REQUIRED)
    @JsonProperty("sessionId") String sessionId,

    @Schema(description = "ISO-8601 timestamp of when the message was created")
    @JsonProperty("timestamp") String timestamp,

    @Schema(description = "Human-readable cancellation notice", defaultValue = "Session was cancelled by the issuer")
    @JsonProperty("message") String message
) {
    public SessionCancelledMessage(String sessionId) {
        this("session_cancelled", sessionId, java.time.Instant.now().toString(), "Session was cancelled by the issuer");
    }
}
