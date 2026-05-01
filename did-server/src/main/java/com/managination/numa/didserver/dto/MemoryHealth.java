package com.managination.numa.didserver.dto;

public record MemoryHealth(
    String status,
    HeapMemory heap,
    NonHeapMemory nonHeap
) {}
