package com.managination.numa.didserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenRequest(
    @JsonProperty("grant_type") String grantType,
    @JsonProperty("pre-authorized_code") String preAuthorizedCode,
    @JsonProperty("user_pin") String userPin
) {}
