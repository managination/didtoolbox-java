package com.managination.numa.didserver.dto;

import com.managination.numa.didserver.model.JsonWebKey;
import java.time.Instant;

public record ServerPublicKeyResponse(
    JsonWebKey publicKey,
    String keyId,
    String algorithm,
    Instant rotatedAt
) {}
