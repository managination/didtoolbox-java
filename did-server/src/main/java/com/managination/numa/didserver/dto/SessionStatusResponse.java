package com.managination.numa.didserver.dto;

import java.time.Instant;

public record SessionStatusResponse(
    String sessionId,
    String status,
    String transportType,
    String holderDid,
    Instant createdAt,
    Instant expiresAt
) {}
