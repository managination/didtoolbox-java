package com.managination.numa.didserver.dto;

import java.util.List;
import java.util.Map;

public record CredentialRequest(
    String format,
    Map<String, List<String>> credentialDefinition,
    Proof proof
) {
    public record Proof(
        String proofType,
        String jwt
    ) {}
}
