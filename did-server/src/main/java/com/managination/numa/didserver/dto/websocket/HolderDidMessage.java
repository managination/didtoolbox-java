package com.managination.numa.didserver.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Sent by the holder to the server, which relays it to the issuer. Contains the holder's DID string.")
public record HolderDidMessage(
    @Schema(description = "Message type identifier", example = "holder_did", requiredMode = REQUIRED)
    @JsonProperty("type") String type,

    @Schema(description = "The session this message belongs to", requiredMode = REQUIRED)
    @JsonProperty("sessionId") String sessionId,

    @Schema(description = "ISO-8601 timestamp of when the message was created")
    @JsonProperty("timestamp") String timestamp,

    @Schema(description = "The holder's DID string (e.g., did:webvh:SCID:holder.did.ninja)", pattern = "^did:webvh:.+", requiredMode = REQUIRED)
    @JsonProperty("did") String did
) {
    public HolderDidMessage(String sessionId, String did) {
        this("holder_did", sessionId, java.time.Instant.now().toString(), did);
    }
}
