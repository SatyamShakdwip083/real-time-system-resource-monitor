package com.systemmonitor.service;

import com.systemmonitor.dto.SystemStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.NetworkIF;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Provides network upload/download speed and totals using OSHI.
 * Speeds are computed by sampling interface stats and differencing over 1s.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NetworkService {

    private final SystemInfo systemInfo;
    private long previousBytesRecv;
    private long previousBytesSent;
    private long previousSampleTime;

    @PostConstruct
    public void init() {
        sampleAndGetTotals();
        previousSampleTime = System.currentTimeMillis();
    }

    /**
     * Returns network stats: bytes received/sent and download/upload bytes per second.
     */
    public SystemStats.NetworkStats getNetworkStats() {
        long now = System.currentTimeMillis();
        long[] totals = sampleAndGetTotals();
        long recv = totals[0];
        long sent = totals[1];
        long elapsedMs = Math.max(1, now - previousSampleTime);
        long downloadPerSec = (long) (1000.0 * (recv - previousBytesRecv) / elapsedMs);
        long uploadPerSec = (long) (1000.0 * (sent - previousBytesSent) / elapsedMs);
        previousBytesRecv = recv;
        previousBytesSent = sent;
        previousSampleTime = now;

        return SystemStats.NetworkStats.builder()
                .downloadBytesPerSecond(Math.max(0, downloadPerSec))
                .uploadBytesPerSecond(Math.max(0, uploadPerSec))
                .totalBytesReceived(recv)
                .totalBytesSent(sent)
                .build();
    }

    /** Returns [totalBytesReceived, totalBytesSent] across all network interfaces. */
    private long[] sampleAndGetTotals() {
        long recv = 0;
        long sent = 0;
        List<NetworkIF> nets = systemInfo.getHardware().getNetworkIFs();
        for (NetworkIF net : nets) {
            net.updateAttributes();
            String name = net.getName();
            if (name == null || (!name.toLowerCase().contains("loopback") && !"lo".equals(name))) {
                recv += net.getBytesRecv();
                sent += net.getBytesSent();
            }
        }
        return new long[]{recv, sent};
    }
}
