package com.managination.numa.didserver.dto;

import java.util.List;

public record ClaimRewardResponse(
    boolean success,
    String message,
    NftTransaction nftTransaction,
    List<String> errors,
    List<String> revokedCredentials
) {}
