package com.systemmonitor.scheduler;

import com.systemmonitor.dto.SystemStats;
import com.systemmonitor.service.CpuService;
import com.systemmonitor.service.DiskService;
import com.systemmonitor.service.GpuService;
import com.systemmonitor.service.MemoryService;
import com.systemmonitor.service.NetworkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsSchedulerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private CpuService cpuService;

    @Mock
    private MemoryService memoryService;

    @Mock
    private GpuService gpuService;

    @Mock
    private DiskService diskService;

    @Mock
    private NetworkService networkService;

    @InjectMocks
    private StatsScheduler statsScheduler;

    @Test
    void pushStats_sendsStatsToTopic() {
        when(cpuService.getCpuStats()).thenReturn(SystemStats.CpuStats.builder()
                .name("Test CPU")
                .usagePercent(10.0)
                .logicalProcessorCount(8)
                .temperatureCelsius(45.0)
                .build());
        when(memoryService.getMemoryStats()).thenReturn(SystemStats.MemoryStats.builder()
                .totalBytes(16_000_000_000L)
                .usedBytes(8_000_000_000L)
                .availableBytes(8_000_000_000L)
                .usagePercent(50.0)
                .build());
        when(gpuService.getGpuStatsList()).thenReturn(Collections.emptyList());
        when(diskService.getDiskStats()).thenReturn(SystemStats.DiskStats.builder()
                .readBytesPerSecond(0)
                .writeBytesPerSecond(0)
                .totalBytes(500_000_000_000L)
                .usedBytes(250_000_000_000L)
                .usagePercent(50.0)
                .build());
        when(networkService.getNetworkStats()).thenReturn(SystemStats.NetworkStats.builder()
                .downloadBytesPerSecond(0)
                .uploadBytesPerSecond(0)
                .totalBytesReceived(0)
                .totalBytesSent(0)
                .build());

        statsScheduler.pushStats();

        ArgumentCaptor<SystemStats> captor = ArgumentCaptor.forClass(SystemStats.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/stats"), captor.capture());
        SystemStats sent = captor.getValue();
        assertThat(sent.getCpu()).isNotNull();
        assertThat(sent.getCpu().getUsagePercent()).isEqualTo(10.0);
        assertThat(sent.getMemory()).isNotNull();
        assertThat(sent.getMemory().getUsagePercent()).isEqualTo(50.0);
        assertThat(sent.getDisk()).isNotNull();
        assertThat(sent.getNetwork()).isNotNull();
        assertThat(sent.getTimestamp()).isPositive();
    }
}
