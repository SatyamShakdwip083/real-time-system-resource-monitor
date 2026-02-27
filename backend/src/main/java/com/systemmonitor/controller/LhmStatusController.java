package com.systemmonitor.controller;

import com.systemmonitor.service.LibreHardwareMonitorService;
import com.systemmonitor.service.LibreHardwareMonitorService.LhmStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Debug endpoint to check if LibreHardwareMonitor is reachable and temps are being read.
 * GET /api/lhm-status → { "reachable": true, "cpuTemp": 48.0, "gpuTemp": 52.0 } or { "reachable": false, "error": "..." }
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LhmStatusController {

    private final LibreHardwareMonitorService libreHardwareMonitorService;

    @GetMapping("/lhm-status")
    public LhmStatus lhmStatus() {
        return libreHardwareMonitorService.getStatus();
    }

    /** Debug: top-level keys in LHM data.json (to fix parser if structure differs). */
    @GetMapping("/lhm-structure")
    public java.util.List<String> lhmStructure() {
        return libreHardwareMonitorService.getJsonTopLevelKeys();
    }

    /** Debug: first 4000 chars of LHM data.json to see actual structure for parser. */
    @GetMapping("/lhm-sample")
    public String lhmSample() {
        return libreHardwareMonitorService.getJsonSample(4000);
    }
}
