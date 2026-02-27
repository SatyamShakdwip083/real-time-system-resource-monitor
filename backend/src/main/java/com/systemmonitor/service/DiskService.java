package com.systemmonitor.service;

import com.systemmonitor.dto.SystemStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Provides disk usage and read/write speed using OSHI.
 * Speeds are computed by sampling disk stats and differencing over 1s.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DiskService {

    private final SystemInfo systemInfo;
    private long previousReadBytes;
    private long previousWriteBytes;
    private long previousSampleTime;

    @PostConstruct
    public void init() {
        sampleAndGetTotals();
        previousSampleTime = System.currentTimeMillis();
    }

    /**
     * Returns disk stats: total/used space and read/write bytes per second.
     */
    public SystemStats.DiskStats getDiskStats() {
        long now = System.currentTimeMillis();
        long[] totals = sampleAndGetTotals();
        long readTotal = totals[0];
        long writeTotal = totals[1];
        long elapsedMs = Math.max(1, now - previousSampleTime);
        long readPerSec = (long) (1000.0 * (readTotal - previousReadBytes) / elapsedMs);
        long writePerSec = (long) (1000.0 * (writeTotal - previousWriteBytes) / elapsedMs);
        previousReadBytes = readTotal;
        previousWriteBytes = writeTotal;
        previousSampleTime = now;

        long totalBytes = 0;
        long usedBytes = 0;
        OperatingSystem os = systemInfo.getOperatingSystem();
        for (OSFileStore fs : os.getFileSystem().getFileStores()) {
            if (fs.getType().toLowerCase().contains("fixed") || fs.getMount().startsWith("/") || fs.getMount().matches("[A-Za-z]:\\\\")) {
                totalBytes += fs.getTotalSpace();
                usedBytes += fs.getTotalSpace() - fs.getUsableSpace();
            }
        }
        if (totalBytes == 0) {
            for (OSFileStore fs : os.getFileSystem().getFileStores()) {
                totalBytes += fs.getTotalSpace();
                usedBytes += fs.getTotalSpace() - fs.getUsableSpace();
            }
        }
        double usagePercent = totalBytes > 0 ? 100.0 * usedBytes / totalBytes : 0.0;
        usagePercent = Math.max(0, Math.min(100, usagePercent));

        return SystemStats.DiskStats.builder()
                .readBytesPerSecond(Math.max(0, readPerSec))
                .writeBytesPerSecond(Math.max(0, writePerSec))
                .totalBytes(totalBytes)
                .usedBytes(usedBytes)
                .usagePercent(round(usagePercent, 2))
                .build();
    }

    /** Returns [totalReadBytes, totalWriteBytes] from all disk stores. */
    private long[] sampleAndGetTotals() {
        long read = 0;
        long write = 0;
        List<HWDiskStore> disks = systemInfo.getHardware().getDiskStores();
        for (HWDiskStore disk : disks) {
            disk.updateAttributes();
            read += disk.getReadBytes();
            write += disk.getWriteBytes();
        }
        return new long[]{read, write};
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        return (double) Math.round(value * factor) / factor;
    }
}
