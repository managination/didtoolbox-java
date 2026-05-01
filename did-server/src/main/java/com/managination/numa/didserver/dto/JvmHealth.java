package com.managination.numa.didserver.dto;

public record JvmHealth(
    int availableProcessors,
    int threadCount,
    int peakThreadCount,
    String uptime,
    String vmName,
    String vmVersion,
    int gcCollections,
    String status
) {}
