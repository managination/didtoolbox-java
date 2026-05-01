package com.managination.numa.didserver.dto;

public record OAuthErrorResponse(
    String error,
    String errorDescription
) {}
