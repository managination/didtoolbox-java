package com.managination.numa.didserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VerificationMethod(
    String id,
    String type,
    String controller,
    String publicKeyMultibase,
    @JsonProperty("publicKeyJwk") JsonWebKey publicKeyJwk
) {}
