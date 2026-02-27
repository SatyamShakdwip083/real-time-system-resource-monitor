package com.systemmonitor.service;

import com.systemmonitor.dto.SystemStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Provides GPU information and usage. Uses LHM per-GPU by SensorId (nvidia/amd),
 * nvidia-smi for NVIDIA when available, and global LHM fallback only for primary GPU.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GpuService {

    private final SystemInfo systemInfo;
    private final LibreHardwareMonitorService libreHardwareMonitorService;
    private final NvidiaSmiService nvidiaSmiService;

    /**
     * Returns stats for all GPUs. Each GPU gets its own temp/usage when available;
     * global LHM is applied only to the primary GPU (NVIDIA first, then AMD) as fallback.
     */
    public List<SystemStats.GpuStats> getGpuStatsList() {
        HardwareAbstractionLayer hal = systemInfo.getHardware();
        List<GraphicsCard> cards = hal.getGraphicsCards();
        List<SystemStats.GpuStats> result = new ArrayList<>();
        if (cards == null || cards.isEmpty()) {
            result.add(SystemStats.GpuStats.builder()
                    .usagePercent(0)
                    .name("N/A")
                    .vramUsedBytes(0)
                    .vramTotalBytes(0)
                    .temperatureCelsius(null)
                    .build());
            return result;
        }
        Double globalLoad = libreHardwareMonitorService.getGpuLoad();
        Double globalTemp = libreHardwareMonitorService.getGpuTemperature();
        int primaryIndex = primaryGpuIndex(cards);

        for (int i = 0; i < cards.size(); i++) {
            GraphicsCard card = cards.get(i);
            String cardName = card.getName() != null ? card.getName() : "";
            long vramTotal = card.getVRam();
            long vramUsed = 0;
            Double temp = libreHardwareMonitorService.getGpuTemperatureByName(cardName);
            Double usage = libreHardwareMonitorService.getGpuLoadByName(cardName);

            boolean isNvidia = cardName.toLowerCase(Locale.ROOT).contains("nvidia") || cardName.toLowerCase(Locale.ROOT).contains("geforce");
            if (isNvidia && nvidiaSmiService.hasData()) {
                double u = nvidiaSmiService.getUsagePercent();
                double t = nvidiaSmiService.getTemperatureCelsius();
                if (!Double.isNaN(u)) usage = u;
                if (!Double.isNaN(t)) temp = t;
            }
            if (i == primaryIndex) {
                if (temp == null) temp = globalTemp;
                if (usage == null) usage = globalLoad;
            }
            double usagePercent = (usage != null) ? usage : 0.0;
            result.add(SystemStats.GpuStats.builder()
                    .usagePercent(round(usagePercent, 2))
                    .name(cardName.isEmpty() ? "N/A" : cardName)
                    .vramUsedBytes(vramUsed)
                    .vramTotalBytes(vramTotal)
                    .temperatureCelsius(temp)
                    .build());
        }
        return result;
    }

    private static int primaryGpuIndex(List<GraphicsCard> cards) {
        if (cards == null || cards.isEmpty()) return 0;
        for (int i = 0; i < cards.size(); i++) {
            String lower = (cards.get(i).getName() != null ? cards.get(i).getName() : "").toLowerCase(Locale.ROOT);
            if (lower.contains("nvidia") || lower.contains("geforce")) return i;
        }
        for (int i = 0; i < cards.size(); i++) {
            String lower = (cards.get(i).getName() != null ? cards.get(i).getName() : "").toLowerCase(Locale.ROOT);
            if (lower.contains("amd") || lower.contains("radeon")) return i;
        }
        return 0;
    }

    /** Returns first GPU stats for backward compatibility. */
    public SystemStats.GpuStats getGpuStats() {
        List<SystemStats.GpuStats> list = getGpuStatsList();
        return list.isEmpty() ? null : list.get(0);
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        return (double) Math.round(value * factor) / factor;
    }
}
