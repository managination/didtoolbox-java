package com.managination.numa.didserver.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Sent by the server to either party when an error occurs during the session.")
public record ErrorMessage(
    @Schema(description = "Message type identifier", example = "error", requiredMode = REQUIRED)
    @JsonProperty("type") String type,

    @Schema(description = "The session this message belongs to", requiredMode = REQUIRED)
    @JsonProperty("sessionId") String sessionId,

    @Schema(description = "ISO-8601 timestamp of when the message was created")
    @JsonProperty("timestamp") String timestamp,

    @Schema(description = "Error code for programmatic handling", allowableValues = {
        "INVALID_MESSAGE", "UNAUTHORIZED", "SESSION_NOT_FOUND", "SESSION_EXPIRED",
        "DUPLICATE_JOIN", "ROLE_MISMATCH", "INVALID_DID", "INVALID_CREDENTIAL",
        "INTERNAL_ERROR", "MESSAGE_TOO_LARGE", "INVALID_FORMAT"
    }, requiredMode = REQUIRED)
    @JsonProperty("code") String code,

    @Schema(description = "Human-readable error description", requiredMode = REQUIRED)
    @JsonProperty("message") String message
) {
    public ErrorMessage(String sessionId, String code, String message) {
        this("error", sessionId, java.time.Instant.now().toString(), code, message);
    }
}
