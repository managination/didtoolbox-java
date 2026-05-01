package com.managination.numa.didserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorResponse(
    String error,
    String message,
    Object details
) {
    public ErrorResponse(String error, String message) {
        this(error, message, null);
    }
}
