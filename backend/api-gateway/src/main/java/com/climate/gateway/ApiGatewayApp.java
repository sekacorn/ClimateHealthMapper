package com.climate.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Main Spring Boot application class for the ClimateHealth API Gateway.
 *
 * <p>This gateway serves as the single entry point for all client requests,
 * handling routing, authentication, authorization, rate limiting, and load balancing
 * across the ClimateHealthMapper microservices ecosystem.
 *
 * <p>Key Features:
 * <ul>
 *   <li>JWT-based authentication and authorization</li>
 *   <li>OAuth2/SSO integration support</li>
 *   <li>Multi-Factor Authentication (MFA) validation</li>
 *   <li>Redis-backed rate limiting</li>
 *   <li>Circuit breaker patterns for resilience</li>
 *   <li>CORS configuration</li>
 *   <li>Request/Response logging and monitoring</li>
 * </ul>
 *
 * @author ClimateHealth Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@SpringBootApplication
@EnableCaching
public class ApiGatewayApp {

    /**
     * Main entry point for the API Gateway application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApp.class, args);
    }
}
