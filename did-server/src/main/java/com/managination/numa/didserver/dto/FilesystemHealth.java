package com.managination.numa.didserver.dto;

public record FilesystemHealth(
    String status,
    String storagePath,
    String availableSpace,
    String reason
) {}
