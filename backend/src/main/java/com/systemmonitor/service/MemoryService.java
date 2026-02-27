package com.systemmonitor.service;

import com.systemmonitor.dto.SystemStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import org.springframework.stereotype.Service;

/**
 * Provides real RAM/memory usage statistics using OSHI.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemoryService {

    private final SystemInfo systemInfo;

    /**
     * Returns current memory usage stats (total, used, available, usage percent).
     */
    public SystemStats.MemoryStats getMemoryStats() {
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;
        double usagePercent = total > 0 ? 100.0 * used / total : 0.0;
        usagePercent = Math.max(0, Math.min(100, usagePercent));
        return SystemStats.MemoryStats.builder()
                .totalBytes(total)
                .usedBytes(used)
                .availableBytes(available)
                .usagePercent(round(usagePercent, 2))
                .build();
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        return (double) Math.round(value * factor) / factor;
    }
}
