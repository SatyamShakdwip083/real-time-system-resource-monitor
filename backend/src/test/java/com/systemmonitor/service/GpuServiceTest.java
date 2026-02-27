package com.systemmonitor.service;

import com.systemmonitor.dto.SystemStats;
import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GpuServiceTest {

    @Mock
    private SystemInfo systemInfo;

    @Mock
    private LibreHardwareMonitorService libreHardwareMonitorService;

    @Mock
    private NvidiaSmiService nvidiaSmiService;

    @Mock
    private HardwareAbstractionLayer hal;

    private GpuService gpuService;

    @BeforeEach
    void setUp() {
        gpuService = new GpuService(systemInfo, libreHardwareMonitorService, nvidiaSmiService);
    }

    @Test
    void getGpuStatsList_whenNoCards_returnsSingleNaEntry() {
        when(systemInfo.getHardware()).thenReturn(hal);
        when(hal.getGraphicsCards()).thenReturn(null);

        List<SystemStats.GpuStats> result = gpuService.getGpuStatsList();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("N/A");
        assertThat(result.get(0).getUsagePercent()).isZero();
        assertThat(result.get(0).getVramTotalBytes()).isZero();
    }

    @Test
    void getGpuStatsList_whenCardsEmpty_returnsSingleNaEntry() {
        when(systemInfo.getHardware()).thenReturn(hal);
        when(hal.getGraphicsCards()).thenReturn(Collections.emptyList());

        List<SystemStats.GpuStats> result = gpuService.getGpuStatsList();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("N/A");
    }

    @Test
    void getGpuStats_whenNoCards_returnsNaEntry() {
        when(systemInfo.getHardware()).thenReturn(hal);
        when(hal.getGraphicsCards()).thenReturn(Collections.emptyList());

        SystemStats.GpuStats result = gpuService.getGpuStats();

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("N/A");
        assertThat(result.getUsagePercent()).isZero();
    }
}
