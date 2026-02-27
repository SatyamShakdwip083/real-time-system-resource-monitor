package com.systemmonitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Reads NVIDIA GPU usage % and temperature via nvidia-smi (cached, non-blocking).
 * Use when LHM per-GPU data is missing so NVIDIA still shows usage.
 */
@Service
@Slf4j
public class NvidiaSmiService {

    private static final long CACHE_MS = 800;
    private volatile double lastUsagePercent = Double.NaN;
    private volatile double lastTempCelsius = Double.NaN;
    private volatile long lastFetchMs = 0;

    /** Returns usage 0–100 or NaN if unavailable. */
    public double getUsagePercent() {
        refreshIfNeeded();
        return lastUsagePercent;
    }

    /** Returns temperature °C or NaN if unavailable. */
    public double getTemperatureCelsius() {
        refreshIfNeeded();
        return lastTempCelsius;
    }

    /** True if we have valid data from nvidia-smi. */
    public boolean hasData() {
        refreshIfNeeded();
        return !Double.isNaN(lastUsagePercent) || !Double.isNaN(lastTempCelsius);
    }

    private void refreshIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastFetchMs < CACHE_MS) return;
        lastFetchMs = now;
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "nvidia-smi",
                    "--query-gpu=utilization.gpu,temperature.gpu",
                    "--format=csv,noheader,nounits"
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            boolean done = proc.waitFor(2, TimeUnit.SECONDS);
            if (!done) {
                proc.destroyForcibly();
                return;
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line = r.readLine();
                if (line == null || line.isBlank()) return;
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    lastUsagePercent = Double.parseDouble(parts[0].trim());
                    lastTempCelsius = Double.parseDouble(parts[1].trim());
                }
            }
        } catch (Exception e) {
            log.trace("nvidia-smi failed: {}", e.getMessage());
            lastUsagePercent = Double.NaN;
            lastTempCelsius = Double.NaN;
        }
    }
}
