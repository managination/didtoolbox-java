package com.managination.numa.didserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") Integer expiresIn,
    @JsonProperty("c_nonce") String cNonce,
    @JsonProperty("c_nonce_expires_in") Integer cNonceExpiresIn,
    @JsonProperty("refresh_token") String refreshToken
) {
    public TokenResponse(String accessToken, String tokenType) {
        this(accessToken, tokenType, null, null, null, null);
    }
}
