package com.systemmonitor.service;

import com.systemmonitor.dto.SystemStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.Sensors;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * Provides real CPU usage statistics using OSHI (JNA-based system access).
 * On Windows, tries OSHI sensors first, then PowerShell WMI fallback for CPU temperature.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CpuService {

    private final SystemInfo systemInfo;
    private final LibreHardwareMonitorService libreHardwareMonitorService;
    private long[] previousTicks;
    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    /** Cached result from Windows PowerShell fallback; refreshed every 3 seconds to avoid slow repeated calls. */
    private volatile Double windowsTempCache = null;
    private volatile long windowsTempCacheTime = 0;
    private static final long CACHE_MS = 3000;

    @PostConstruct
    public void init() {
        previousTicks = systemInfo.getHardware().getProcessor().getSystemCpuLoadTicks();
    }

    /**
     * Returns current CPU usage stats (0-100%), logical processor count, and real-time temperature.
     */
    public SystemStats.CpuStats getCpuStats() {
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        long[] ticks = processor.getSystemCpuLoadTicks();
        double usagePercent = 0.0;
        if (previousTicks != null && ticks != null && ticks.length == previousTicks.length) {
            long totalDiff = 0;
            long idleDiff = ticks[CentralProcessor.TickType.IDLE.getIndex()]
                    - previousTicks[CentralProcessor.TickType.IDLE.getIndex()];
            for (int i = 0; i < ticks.length; i++) {
                totalDiff += ticks[i] - previousTicks[i];
            }
            if (totalDiff > 0) {
                usagePercent = 100.0 * (1.0 - (double) idleDiff / totalDiff);
            }
        }
        previousTicks = ticks;
        usagePercent = Math.max(0, Math.min(100, usagePercent));

        Double tempCelsius = getCpuTemperatureOshi();
        if (tempCelsius == null) {
            tempCelsius = libreHardwareMonitorService.getCpuTemperature();
        }
        if (tempCelsius == null && IS_WINDOWS) {
            tempCelsius = getCpuTemperatureWindowsFallback();
        }

        String cpuName = null;
        try {
            if (processor.getProcessorIdentifier() != null) {
                cpuName = processor.getProcessorIdentifier().getName();
                if (cpuName != null) cpuName = cpuName.trim();
                if (cpuName != null && cpuName.isEmpty()) cpuName = null;
            }
        } catch (Exception e) {
            log.trace("CPU name not available: {}", e.getMessage());
        }
        if (cpuName == null) cpuName = "N/A";

        return SystemStats.CpuStats.builder()
                .name(cpuName)
                .usagePercent(round(usagePercent, 2))
                .logicalProcessorCount(processor.getLogicalProcessorCount())
                .temperatureCelsius(tempCelsius)
                .build();
    }

    private Double getCpuTemperatureOshi() {
        try {
            Sensors sensors = systemInfo.getHardware().getSensors();
            if (sensors != null) {
                double t = sensors.getCpuTemperature();
                if (!Double.isNaN(t) && t > 0 && t < 150) {
                    return round(t, 1);
                }
            }
        } catch (Exception e) {
            log.trace("CPU temperature (OSHI) not available: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Windows fallback: run PowerShell to read MSAcpi_ThermalZoneTemperature (tenths of Kelvin).
     * Result cached for 3 seconds because PowerShell is slow. Works only if BIOS exposes this WMI class.
     */
    private Double getCpuTemperatureWindowsFallback() {
        long now = System.currentTimeMillis();
        if (windowsTempCache != null && (now - windowsTempCacheTime) < CACHE_MS) {
            return windowsTempCache;
        }
        Double result = readCpuTemperatureViaPowerShell();
        windowsTempCacheTime = now;
        windowsTempCache = result;
        return result;
    }

    private Double readCpuTemperatureViaPowerShell() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-NoProfile", "-NonInteractive", "-Command",
                    "try { $t = (Get-CimInstance -ClassName MSAcpi_ThermalZoneTemperature -Namespace root/wmi -ErrorAction Stop | Select-Object -First 1).CurrentTemperature; [math]::Round(($t/10.0)-273.15,1) } catch { '' }"
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                if (line != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        double val = Double.parseDouble(line);
                        if (val > 0 && val < 150) {
                            return round(val, 1);
                        }
                    }
                }
            }
            p.waitFor();
        } catch (Exception e) {
            log.trace("CPU temperature (Windows fallback) not available: {}", e.getMessage());
        }
        return null;
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        return (double) Math.round(value * factor) / factor;
    }
}
