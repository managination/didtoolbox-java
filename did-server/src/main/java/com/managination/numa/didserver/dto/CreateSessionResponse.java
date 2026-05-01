package com.managination.numa.didserver.dto;

import java.time.Instant;

public record CreateSessionResponse(
    String sessionId,
    String wsUrl,
    String qrCodeData,
    Instant expiresAt
) {}
