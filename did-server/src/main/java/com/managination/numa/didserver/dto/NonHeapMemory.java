package com.managination.numa.didserver.dto;

public record NonHeapMemory(
    String used,
    String committed
) {}
