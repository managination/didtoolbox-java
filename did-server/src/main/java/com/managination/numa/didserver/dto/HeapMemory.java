package com.managination.numa.didserver.dto;

public record HeapMemory(
    String max,
    String used,
    String committed,
    String usagePercent
) {}
