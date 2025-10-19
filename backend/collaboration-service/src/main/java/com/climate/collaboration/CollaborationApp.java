package com.climate.collaboration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Collaboration Service
 * Provides real-time collaboration features for ClimateHealthMapper
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class CollaborationApp {

    public static void main(String[] args) {
        SpringApplication.run(CollaborationApp.class, args);
    }
}
