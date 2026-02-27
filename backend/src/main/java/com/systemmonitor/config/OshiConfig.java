package com.systemmonitor.config;

import oshi.SystemInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides OSHI SystemInfo bean for system-level metric collection.
 */
@Configuration
public class OshiConfig {

    @Bean
    public SystemInfo systemInfo() {
        return new SystemInfo();
    }
}
