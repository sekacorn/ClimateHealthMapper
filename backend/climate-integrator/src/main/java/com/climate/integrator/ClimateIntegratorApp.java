package com.climate.integrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Climate Integrator Microservice
 *
 * This service handles the integration of multiple data sources:
 * - Environmental data (NOAA, EPA) in CSV/JSON format
 * - Health data in FHIR format
 * - Genomic data in VCF format
 *
 * @author ClimateHealthMapper Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
public class ClimateIntegratorApp {

    public static void main(String[] args) {
        SpringApplication.run(ClimateIntegratorApp.class, args);
    }
}
