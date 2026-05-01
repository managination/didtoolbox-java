package com.managination.numa.didserver.dto;

import java.util.List;

public record PresentationVerificationResponse(
    boolean verified,
    List<String> errors,
    String holderDid,
    List<CredentialInfo> credentials
) {
    public record CredentialInfo(
        String issuer,
        List<String> type,
        boolean verified
    ) {}
}
