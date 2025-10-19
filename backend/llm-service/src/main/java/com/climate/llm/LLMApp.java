package com.climate.llm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for ClimateHealth LLM Service
 * Provides MBTI-tailored natural language processing and troubleshooting
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableAsync
public class LLMApp {

    public static void main(String[] args) {
        SpringApplication.run(LLMApp.class, args);
    }
}
