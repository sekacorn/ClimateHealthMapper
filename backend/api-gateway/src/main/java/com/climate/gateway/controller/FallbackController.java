package com.climate.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback controller for handling circuit breaker fallbacks.
 *
 * <p>This controller provides fallback responses when downstream services
 * are unavailable or when circuit breakers are open. It ensures graceful
 * degradation and provides meaningful error messages to clients.
 *
 * @author ClimateHealth Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * Fallback for Climate Integrator Service.
     *
     * @return fallback response
     */
    @GetMapping("/integrator")
    public ResponseEntity<Map<String, Object>> integratorFallback() {
        log.warn("Integrator service fallback triggered");
        return createFallbackResponse(
            "Climate Integrator Service",
            "The climate data integration service is temporarily unavailable. Please try again later."
        );
    }

    /**
     * Fallback for Climate Visualizer Service.
     *
     * @return fallback response
     */
    @GetMapping("/visualizer")
    public ResponseEntity<Map<String, Object>> visualizerFallback() {
        log.warn("Visualizer service fallback triggered");
        return createFallbackResponse(
            "Climate Visualizer Service",
            "The data visualization service is temporarily unavailable. Please try again later."
        );
    }

    /**
     * Fallback for User Session Service.
     *
     * @return fallback response
     */
    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> sessionFallback() {
        log.warn("Session service fallback triggered");
        return createFallbackResponse(
            "User Session Service",
            "The authentication service is temporarily unavailable. Please try again later."
        );
    }

    /**
     * Fallback for LLM Service.
     *
     * @return fallback response
     */
    @GetMapping("/llm")
    public ResponseEntity<Map<String, Object>> llmFallback() {
        log.warn("LLM service fallback triggered");
        return createFallbackResponse(
            "LLM Service",
            "The AI/ML service is temporarily unavailable. Please try again later."
        );
    }

    /**
     * Fallback for Collaboration Service.
     *
     * @return fallback response
     */
    @GetMapping("/collaboration")
    public ResponseEntity<Map<String, Object>> collaborationFallback() {
        log.warn("Collaboration service fallback triggered");
        return createFallbackResponse(
            "Collaboration Service",
            "The collaboration service is temporarily unavailable. Please try again later."
        );
    }

    /**
     * Creates a standardized fallback response.
     *
     * @param serviceName the name of the unavailable service
     * @param message the error message
     * @return the fallback response
     */
    private ResponseEntity<Map<String, Object>> createFallbackResponse(String serviceName, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Service Unavailable");
        response.put("service", serviceName);
        response.put("message", message);
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("timestamp", Instant.now().toString());
        response.put("recommendation", "Please try again in a few moments or contact support if the issue persists.");

        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(response);
    }
}
