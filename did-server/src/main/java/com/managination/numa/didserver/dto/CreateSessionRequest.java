package com.managination.numa.didserver.dto;

import java.time.Instant;

public record CreateSessionRequest(
    String transportType,
    String credentialType,
    Integer timeout
) {
    public int getTimeoutOrDefault() {
        return timeout != null ? timeout : 300;
    }
}
