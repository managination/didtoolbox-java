package com.managination.numa.didserver.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SessionStatus {
    PENDING("pending"),
    HOLDER_CONNECTED("holder_connected"),
    CREDENTIAL_ISSUED("credential_issued"),
    COMPLETED("completed"),
    EXPIRED("expired"),
    CANCELLED("cancelled");

    private final String value;

    SessionStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static SessionStatus fromValue(String value) {
        for (SessionStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown SessionStatus: " + value);
    }
}
