package com.systemmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Real-Time System Resource Monitor backend.
 * Enables WebSocket stats streaming and scheduled metric collection.
 */
@SpringBootApplication
@EnableScheduling
public class SystemMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemMonitorApplication.class, args);
    }
}
