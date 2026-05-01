package com.managination.numa.didserver.dto;

public record EnvironmentHealth(
    String tempDir,
    String userDir,
    String fileEncoding,
    String defaultLocale,
    String status
) {}
