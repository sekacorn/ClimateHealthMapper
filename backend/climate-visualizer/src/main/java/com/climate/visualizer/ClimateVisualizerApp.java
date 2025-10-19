package com.climate.visualizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Climate Visualizer Microservice
 *
 * Provides 3D climate visualization and health heatmap generation
 * with MBTI-specific styling and resource monitoring.
 *
 * @author ClimateHealthMapper Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class ClimateVisualizerApp {

    public static void main(String[] args) {
        SpringApplication.run(ClimateVisualizerApp.class, args);
    }
}
