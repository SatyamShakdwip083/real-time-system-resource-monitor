package com.systemmonitor.controller;

import com.systemmonitor.dto.ProcessInfo;
import com.systemmonitor.service.ProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for per-application (process) resource usage.
 * Used when the user clicks a resource card (CPU, RAM, etc.) to see which apps use it.
 */
@RestController
@RequestMapping("/api/processes")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;

    /**
     * GET /api/processes?sort=cpu|memory|disk&limit=25
     * Returns top processes sorted by CPU, memory, or disk I/O only.
     */
    @GetMapping
    public ResponseEntity<List<ProcessInfo>> getTopProcesses(
            @RequestParam(defaultValue = "cpu") String sort,
            @RequestParam(defaultValue = "25") int limit) {
        if (!"cpu".equalsIgnoreCase(sort) && !"memory".equalsIgnoreCase(sort) && !"disk".equalsIgnoreCase(sort)) {
            sort = "cpu";
        }
        List<ProcessInfo> list = processService.getTopProcesses(sort, Math.min(100, Math.max(1, limit)));
        return ResponseEntity.ok(list);
    }
}
