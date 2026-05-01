package com.managination.numa.didserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DidDocument(
    @JsonProperty("@context") Object context,
    String id,
    Object controller,
    List<String> alsoKnownAs,
    List<VerificationMethod> verificationMethod,
    List<Object> authentication,
    List<Object> assertionMethod,
    List<Object> keyAgreement,
    List<Object> capabilityInvocation,
    List<Object> capabilityDelegation,
    List<ServiceEndpoint> service
) {}
