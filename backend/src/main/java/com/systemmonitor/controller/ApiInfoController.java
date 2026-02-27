package com.systemmonitor.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes app version and name for the frontend (e.g. footer).
 */
@RestController
@RequestMapping("/api")
public class ApiInfoController {

    @Value("${info.app.version:1.0.0}")
    private String version;

    @Value("${spring.application.name:system-monitor-backend}")
    private String name;

    @GetMapping("/info")
    public Map<String, String> info() {
        return Map.of(
                "version", version,
                "name", name
        );
    }
}
