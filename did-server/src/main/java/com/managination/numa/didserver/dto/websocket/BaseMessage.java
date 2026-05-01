package com.managination.numa.didserver.dto.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BaseMessage(
    @JsonProperty("type") String type,
    @JsonProperty("sessionId") String sessionId,
    @JsonProperty("timestamp") String timestamp
) {
    public BaseMessage(String type, String sessionId) {
        this(type, sessionId, java.time.Instant.now().toString());
    }
}
