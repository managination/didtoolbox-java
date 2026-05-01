package com.managination.numa.didserver.dto;

import com.managination.numa.didserver.model.DidDocument;

public record DidRegistrationRequest(
    String did,
    DidDocument document,
    java.util.List<java.util.Map<String, Object>> log
) {}
