package com.systemmonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Unified DTO containing all system resource metrics.
 * Serialized as JSON and pushed to WebSocket clients every second.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStats implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Timestamp when stats were collected (epoch millis). */
    private long timestamp;

    /** CPU usage percentage (0-100). */
    private CpuStats cpu;

    /** RAM / memory usage statistics. */
    private MemoryStats memory;

    /** GPU usage statistics (first GPU; use gpus for multiple). */
    private GpuStats gpu;
    /** All GPUs (when multiple are present). */
    private java.util.List<GpuStats> gpus;

    /** Disk read/write and usage statistics. */
    private DiskStats disk;

    /** Network upload/download statistics. */
    private NetworkStats network;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CpuStats implements Serializable {
        private static final long serialVersionUID = 1L;
        /** CPU model name (e.g. "Intel Core i7-9700K"). */
        private String name;
        /** Overall CPU usage percentage (0-100). */
        private double usagePercent;
        /** Number of logical processors. */
        private int logicalProcessorCount;
        /** CPU temperature in °C; null if unavailable. */
        private Double temperatureCelsius;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryStats implements Serializable {
        private static final long serialVersionUID = 1L;
        /** Total physical memory in bytes. */
        private long totalBytes;
        /** Used (including OS) memory in bytes. */
        private long usedBytes;
        /** Available memory in bytes. */
        private long availableBytes;
        /** Usage percentage (0-100). */
        private double usagePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GpuStats implements Serializable {
        private static final long serialVersionUID = 1L;
        /** GPU usage percentage (0-100). 0 if not available. */
        private double usagePercent;
        /** GPU name or "N/A". */
        private String name;
        /** VRAM used in bytes (0 if N/A). */
        private long vramUsedBytes;
        /** VRAM total in bytes (0 if N/A). */
        private long vramTotalBytes;
        /** GPU temperature in °C; null if unavailable (OSHI does not provide this on all platforms). */
        private Double temperatureCelsius;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiskStats implements Serializable {
        private static final long serialVersionUID = 1L;
        /** Read speed in bytes per second (since last sample). */
        private long readBytesPerSecond;
        /** Write speed in bytes per second (since last sample). */
        private long writeBytesPerSecond;
        /** Total disk space in bytes. */
        private long totalBytes;
        /** Used disk space in bytes. */
        private long usedBytes;
        /** Usage percentage (0-100). */
        private double usagePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NetworkStats implements Serializable {
        private static final long serialVersionUID = 1L;
        /** Download speed in bytes per second (since last sample). */
        private long downloadBytesPerSecond;
        /** Upload speed in bytes per second (since last sample). */
        private long uploadBytesPerSecond;
        /** Total bytes received (cumulative). */
        private long totalBytesReceived;
        /** Total bytes sent (cumulative). */
        private long totalBytesSent;
    }
}
