package com.managination.numa.didserver.dto;

public record DidUpdateResponse(
    boolean success,
    String versionId,
    String message
) {}
