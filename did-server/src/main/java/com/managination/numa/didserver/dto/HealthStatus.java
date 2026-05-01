package com.managination.numa.didserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record HealthStatus(
    String status,
    Instant timestamp,
    FilesystemHealth filesystem,
    MemoryHealth memory,
    JvmHealth jvm,
    EnvironmentHealth environment
) {}
