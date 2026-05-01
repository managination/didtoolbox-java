package com.managination.numa.didserver.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Sent by either the issuer or holder when first connecting to the session.")
public record JoinMessage(
    @Schema(description = "Message type identifier", example = "join", requiredMode = REQUIRED)
    @JsonProperty("type") String type,

    @Schema(description = "The session this message belongs to", requiredMode = REQUIRED)
    @JsonProperty("sessionId") String sessionId,

    @Schema(description = "ISO-8601 timestamp of when the message was created")
    @JsonProperty("timestamp") String timestamp,

    @Schema(description = "The role of the connecting party", allowableValues = {"issuer", "holder"}, requiredMode = REQUIRED)
    @JsonProperty("role") String role
) {
    public JoinMessage(String sessionId, String role) {
        this("join", sessionId, java.time.Instant.now().toString(), role);
    }
}
