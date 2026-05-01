package com.managination.numa.didserver.dto.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Base message structure for all WebSocket messages.")
public record BaseMessage(
    @Schema(description = "Message type identifier", requiredMode = REQUIRED)
    @JsonProperty("type") String type,

    @Schema(description = "The session this message belongs to", requiredMode = REQUIRED)
    @JsonProperty("sessionId") String sessionId,

    @Schema(description = "ISO-8601 timestamp of when the message was created")
    @JsonProperty("timestamp") String timestamp
) {
    public BaseMessage(String type, String sessionId) {
        this(type, sessionId, java.time.Instant.now().toString());
    }
}
