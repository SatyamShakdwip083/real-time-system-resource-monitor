package com.systemmonitor.scheduler;

import com.systemmonitor.dto.SystemStats;
import com.systemmonitor.service.CpuService;
import com.systemmonitor.service.DiskService;
import com.systemmonitor.service.GpuService;
import com.systemmonitor.service.MemoryService;
import com.systemmonitor.service.NetworkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs every second (1000 ms), aggregates all system stats and pushes
 * a single JSON message to WebSocket topic /topic/stats.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StatsScheduler {

    private final SimpMessagingTemplate messagingTemplate;
    private final CpuService cpuService;
    private final MemoryService memoryService;
    private final GpuService gpuService;
    private final DiskService diskService;
    private final NetworkService networkService;

    @Scheduled(fixedRate = 1000)
    public void pushStats() {
        java.util.List<SystemStats.GpuStats> gpus;
        try {
            gpus = gpuService.getGpuStatsList();
        } catch (Exception e) {
            log.warn("GPU stats failed, using placeholder: {}", e.getMessage());
            gpus = java.util.List.of(SystemStats.GpuStats.builder()
                    .usagePercent(0)
                    .name("N/A")
                    .vramUsedBytes(0)
                    .vramTotalBytes(0)
                    .temperatureCelsius(null)
                    .build());
        }
        try {
            SystemStats stats = SystemStats.builder()
                    .timestamp(System.currentTimeMillis())
                    .cpu(cpuService.getCpuStats())
                    .memory(memoryService.getMemoryStats())
                    .gpu(gpus.isEmpty() ? null : gpus.get(0))
                    .gpus(gpus)
                    .disk(diskService.getDiskStats())
                    .network(networkService.getNetworkStats())
                    .build();
            messagingTemplate.convertAndSend("/topic/stats", stats);
        } catch (Exception e) {
            log.warn("Failed to collect or send stats: {}", e.getMessage());
        }
    }
}
