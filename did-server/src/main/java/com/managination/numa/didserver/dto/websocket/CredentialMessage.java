package com.managination.numa.didserver.dto.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CredentialMessage(
    @JsonProperty("type") String type,
    @JsonProperty("sessionId") String sessionId,
    @JsonProperty("timestamp") String timestamp,
    @JsonProperty("credential") String credential,
    @JsonProperty("format") String format
) {
    public CredentialMessage(String sessionId, String credential) {
        this("credential", sessionId, java.time.Instant.now().toString(), credential, "jwt_vc_json");
    }

    public CredentialMessage(String sessionId, String credential, String format) {
        this("credential", sessionId, java.time.Instant.now().toString(), credential, format);
    }
}
