package com.climate.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FallbackController.
 *
 * @author ClimateHealth Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@SpringBootTest
@ActiveProfiles("test")
class FallbackControllerTest {

    @Autowired
    private FallbackController fallbackController;

    @Test
    void testIntegratorFallback() {
        // When
        ResponseEntity<Map<String, Object>> response = fallbackController.integratorFallback();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> body = response.getBody();
        assertEquals("Service Unavailable", body.get("error"));
        assertEquals("Climate Integrator Service", body.get("service"));
        assertEquals(503, body.get("status"));
        assertNotNull(body.get("message"));
        assertNotNull(body.get("timestamp"));
        assertNotNull(body.get("recommendation"));
    }

    @Test
    void testVisualizerFallback() {
        // When
        ResponseEntity<Map<String, Object>> response = fallbackController.visualizerFallback();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> body = response.getBody();
        assertEquals("Climate Visualizer Service", body.get("service"));
    }

    @Test
    void testSessionFallback() {
        // When
        ResponseEntity<Map<String, Object>> response = fallbackController.sessionFallback();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> body = response.getBody();
        assertEquals("User Session Service", body.get("service"));
    }

    @Test
    void testLlmFallback() {
        // When
        ResponseEntity<Map<String, Object>> response = fallbackController.llmFallback();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> body = response.getBody();
        assertEquals("LLM Service", body.get("service"));
    }

    @Test
    void testCollaborationFallback() {
        // When
        ResponseEntity<Map<String, Object>> response = fallbackController.collaborationFallback();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> body = response.getBody();
        assertEquals("Collaboration Service", body.get("service"));
    }
}
