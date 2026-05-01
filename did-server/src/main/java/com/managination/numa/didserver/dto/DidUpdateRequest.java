package com.managination.numa.didserver.dto;

import com.managination.numa.didserver.model.DidDocument;
import java.util.Map;

public record DidUpdateRequest(
    DidDocument document,
    Map<String, Object> logEntry,
    String versionId
) {}
