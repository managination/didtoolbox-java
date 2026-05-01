package com.managination.numa.didserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ServiceEndpoint(
    String id,
    Object type,
    @JsonProperty("serviceEndpoint") Object serviceEndpoint
) {}
