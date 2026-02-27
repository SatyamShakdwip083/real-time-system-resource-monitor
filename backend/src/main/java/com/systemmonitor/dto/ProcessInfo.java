package com.systemmonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-application (process) resource usage for the process list API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessInfo {
    private int pid;
    private String name;
    /** CPU usage 0-100 (best effort; may be 0 on some platforms). */
    private double cpuPercent;
    /** Resident memory in bytes. */
    private long memoryBytes;
    /** Optional: disk read bytes (if available). */
    private long diskReadBytes;
    /** Optional: disk write bytes (if available). */
    private long diskWriteBytes;
}
