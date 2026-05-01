package com.managination.numa.didserver.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HolderDidMessage(
    @JsonProperty("type") String type,
    @JsonProperty("sessionId") String sessionId,
    @JsonProperty("timestamp") String timestamp,
    @JsonProperty("did") String did
) {
    public HolderDidMessage(String sessionId, String did) {
        this("holder_did", sessionId, java.time.Instant.now().toString(), did);
    }
}
