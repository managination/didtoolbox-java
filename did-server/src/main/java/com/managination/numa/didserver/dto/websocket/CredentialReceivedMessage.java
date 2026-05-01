package com.managination.numa.didserver.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Sent by the server to the issuer to confirm the holder has received the credential.")
public record CredentialReceivedMessage(
    @Schema(description = "Message type identifier", example = "credential_received", requiredMode = REQUIRED)
    @JsonProperty("type") String type,

    @Schema(description = "The session this message belongs to", requiredMode = REQUIRED)
    @JsonProperty("sessionId") String sessionId,

    @Schema(description = "ISO-8601 timestamp of when the message was created")
    @JsonProperty("timestamp") String timestamp
) {
    public CredentialReceivedMessage(String sessionId) {
        this("credential_received", sessionId, java.time.Instant.now().toString());
    }
}
