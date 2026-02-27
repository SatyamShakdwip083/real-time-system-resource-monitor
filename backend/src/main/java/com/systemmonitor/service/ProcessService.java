package com.systemmonitor.service;

import com.systemmonitor.dto.ProcessInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Returns top processes by CPU, memory, or disk I/O for the "applications using this resource" view.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessService {

    private final SystemInfo systemInfo;

    private static final int DEFAULT_LIMIT = 25;
    private static final int DISK_FETCH_LIMIT = 500;

    /**
     * Top processes sorted by the given resource only.
     * @param sort "cpu" (CPU-using apps), "memory" (RAM-using apps), or "disk" (disk I/O)
     * @param limit max number of processes (default 25)
     */
    public List<ProcessInfo> getTopProcesses(String sort, int limit) {
        if (limit <= 0) limit = DEFAULT_LIMIT;
        OperatingSystem os = systemInfo.getOperatingSystem();
        List<OSProcess> processes;
        if ("memory".equalsIgnoreCase(sort)) {
            processes = os.getProcesses(null, OperatingSystem.ProcessSorting.RSS_DESC, limit);
        } else if ("disk".equalsIgnoreCase(sort)) {
            List<OSProcess> all = os.getProcesses(null, null, DISK_FETCH_LIMIT);
            processes = all.stream()
                    .filter(p -> p != null && p.getProcessID() > 0)
                    .sorted((a, b) -> Long.compare(
                            (b.getBytesRead() >= 0 ? b.getBytesRead() : 0) + (b.getBytesWritten() >= 0 ? b.getBytesWritten() : 0),
                            (a.getBytesRead() >= 0 ? a.getBytesRead() : 0) + (a.getBytesWritten() >= 0 ? a.getBytesWritten() : 0)))
                    .limit(limit)
                    .toList();
        } else {
            processes = os.getProcesses(null, OperatingSystem.ProcessSorting.CPU_DESC, limit);
        }
        return processes.stream()
                .filter(p -> p != null && p.getProcessID() > 0)
                .map(this::toProcessInfo)
                .collect(Collectors.toList());
    }

    private ProcessInfo toProcessInfo(OSProcess p) {
        String name = p.getName();
        if (name == null || name.isBlank()) name = "[" + p.getProcessID() + "]";
        long rss = p.getResidentSetSize() > 0 ? p.getResidentSetSize() : 0;
        double cpu = p.getProcessCpuLoadCumulative() >= 0 ? p.getProcessCpuLoadCumulative() * 100.0 : 0.0;
        return ProcessInfo.builder()
                .pid(p.getProcessID())
                .name(name)
                .cpuPercent(Math.min(100, Math.max(0, cpu)))
                .memoryBytes(rss)
                .diskReadBytes(p.getBytesRead() >= 0 ? p.getBytesRead() : 0)
                .diskWriteBytes(p.getBytesWritten() >= 0 ? p.getBytesWritten() : 0)
                .build();
    }
}
