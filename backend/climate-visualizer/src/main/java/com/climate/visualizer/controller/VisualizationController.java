package com.climate.visualizer.controller;

import com.climate.visualizer.model.Visualization;
import com.climate.visualizer.service.ExportService;
import com.climate.visualizer.service.ResourceMonitorService;
import com.climate.visualizer.service.VisualizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Visualization Operations
 *
 * Provides endpoints for generating, retrieving, and exporting
 * climate and health visualizations with resource monitoring.
 */
@RestController
@RequestMapping("/api/visualizer")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class VisualizationController {

    private final VisualizationService visualizationService;
    private final ExportService exportService;
    private final ResourceMonitorService resourceMonitorService;

    /**
     * Generate 3D Climate Visualization
     */
    @PostMapping("/generate/climate-map")
    public ResponseEntity<Map<String, Object>> generateClimateMap(
            @Valid @RequestBody ClimateMapRequest request) {
        try {
            log.info("Generating climate map for user: {}, location: ({}, {})",
                     request.getUserId(), request.getLatitude(), request.getLongitude());

            Map<String, Object> result = visualizationService.generateClimateMap(
                request.getUserId(),
                request.getLatitude(),
                request.getLongitude(),
                request.getRadius(),
                request.getMbtiType(),
                request.getQuality()
            );

            log.info("Climate map generated successfully with ID: {}", result.get("visualizationId"));
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating climate map", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate climate map: " + e.getMessage()));
        }
    }

    /**
     * Generate Health Risk Heatmap
     */
    @PostMapping("/generate/health-heatmap")
    public ResponseEntity<Map<String, Object>> generateHealthHeatmap(
            @Valid @RequestBody HealthHeatmapRequest request) {
        try {
            log.info("Generating health heatmap for user: {}, location: ({}, {})",
                     request.getUserId(), request.getLatitude(), request.getLongitude());

            Map<String, Object> result = visualizationService.generateHealthHeatmap(
                request.getUserId(),
                request.getLatitude(),
                request.getLongitude(),
                request.getRadius(),
                request.getHealthFactors(),
                request.getMbtiType(),
                request.getQuality()
            );

            log.info("Health heatmap generated successfully with ID: {}", result.get("visualizationId"));
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating health heatmap", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate health heatmap: " + e.getMessage()));
        }
    }

    /**
     * Get all visualizations for a user
     */
    @GetMapping("/visualizations/{userId}")
    public ResponseEntity<List<Visualization>> getUserVisualizations(
            @PathVariable String userId,
            @RequestParam(required = false) String type) {
        try {
            log.info("Fetching visualizations for user: {}, type: {}", userId, type);

            List<Visualization> visualizations = visualizationService.getUserVisualizations(userId, type);

            return ResponseEntity.ok(visualizations);

        } catch (Exception e) {
            log.error("Error fetching user visualizations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a specific visualization by ID
     */
    @GetMapping("/visualization/{id}")
    public ResponseEntity<Visualization> getVisualization(@PathVariable Long id) {
        try {
            log.info("Fetching visualization with ID: {}", id);

            return visualizationService.getVisualization(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            log.error("Error fetching visualization", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Export visualization to specified format
     */
    @PostMapping("/export/{id}")
    public ResponseEntity<Map<String, Object>> exportVisualization(
            @PathVariable Long id,
            @RequestBody ExportRequest request) {
        try {
            log.info("Exporting visualization {} to format: {}", id, request.getFormat());

            Map<String, Object> result = exportService.exportVisualization(
                id,
                request.getFormat(),
                request.getOptions()
            );

            log.info("Visualization exported successfully to: {}", result.get("filePath"));
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.error("Invalid export request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error exporting visualization", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to export visualization: " + e.getMessage()));
        }
    }

    /**
     * Check system resources (CPU, Memory, GPU)
     */
    @GetMapping("/resources/check")
    public ResponseEntity<Map<String, Object>> checkResources() {
        try {
            log.debug("Checking system resources");

            Map<String, Object> resources = resourceMonitorService.getSystemResources();

            return ResponseEntity.ok(resources);

        } catch (Exception e) {
            log.error("Error checking system resources", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to check resources: " + e.getMessage()));
        }
    }

    /**
     * Delete a visualization
     */
    @DeleteMapping("/visualization/{id}")
    public ResponseEntity<Map<String, String>> deleteVisualization(@PathVariable Long id) {
        try {
            log.info("Deleting visualization with ID: {}", id);

            visualizationService.deleteVisualization(id);

            return ResponseEntity.ok(Map.of("message", "Visualization deleted successfully"));

        } catch (Exception e) {
            log.error("Error deleting visualization", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete visualization: " + e.getMessage()));
        }
    }

    // Request DTOs
    @lombok.Data
    public static class ClimateMapRequest {
        private String userId;
        private Double latitude;
        private Double longitude;
        private Double radius = 50.0; // km
        private String mbtiType;
        private String quality = "medium";
    }

    @lombok.Data
    public static class HealthHeatmapRequest {
        private String userId;
        private Double latitude;
        private Double longitude;
        private Double radius = 50.0; // km
        private List<String> healthFactors;
        private String mbtiType;
        private String quality = "medium";
    }

    @lombok.Data
    public static class ExportRequest {
        private String format; // PNG, SVG, STL
        private Map<String, Object> options;
    }
}
