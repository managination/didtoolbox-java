package com.managination.numa.didserver.dto.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Sent by the issuer to the server, which relays it to the holder. Contains the signed JWT verifiable credential.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CredentialMessage(
    @Schema(description = "Message type identifier", example = "credential", requiredMode = REQUIRED)
    @JsonProperty("type") String type,

    @Schema(description = "The session this message belongs to", requiredMode = REQUIRED)
    @JsonProperty("sessionId") String sessionId,

    @Schema(description = "ISO-8601 timestamp of when the message was created")
    @JsonProperty("timestamp") String timestamp,

    @Schema(description = "The signed JWT verifiable credential", requiredMode = REQUIRED)
    @JsonProperty("credential") String credential,

    @Schema(description = "Credential format (e.g., jwt_vc_json)", defaultValue = "jwt_vc_json")
    @JsonProperty("format") String format
) {
    public CredentialMessage(String sessionId, String credential) {
        this("credential", sessionId, java.time.Instant.now().toString(), credential, "jwt_vc_json");
    }

    public CredentialMessage(String sessionId, String credential, String format) {
        this("credential", sessionId, java.time.Instant.now().toString(), credential, format);
    }
}
