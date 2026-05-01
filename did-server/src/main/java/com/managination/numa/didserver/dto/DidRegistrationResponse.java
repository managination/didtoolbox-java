package com.managination.numa.didserver.dto;

public record DidRegistrationResponse(
    boolean success,
    String did,
    String message
) {}
