package com.climate.visualizer.service;

import com.climate.visualizer.model.ClimateMap;
import com.climate.visualizer.model.HealthMap;
import com.climate.visualizer.model.Visualization;
import com.climate.visualizer.repository.VisualizationRepository;
import com.climate.visualizer.utils.MultithreadingManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for generating 3D visualizations
 *
 * Creates Three.js-compatible JSON output for climate maps
 * and health heatmaps with MBTI-specific styling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VisualizationService {

    private final VisualizationRepository visualizationRepository;
    private final MbtiStyleService mbtiStyleService;
    private final MultithreadingManager multithreadingManager;
    private final ResourceMonitorService resourceMonitorService;
    private final ObjectMapper objectMapper;

    /**
     * Generate 3D Climate Map Visualization
     */
    @Transactional
    public Map<String, Object> generateClimateMap(
            String userId,
            Double latitude,
            Double longitude,
            Double radius,
            String mbtiType,
            String quality) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting climate map generation for location: ({}, {}), radius: {}km",
                     latitude, longitude, radius);

            // Validate inputs
            validateCoordinates(latitude, longitude);
            validateMbtiType(mbtiType);

            // Generate climate data points
            List<ClimateDataPoint> dataPoints = generateClimateDataPoints(
                latitude, longitude, radius, quality);

            // Process data in parallel using multithreading
            CompletableFuture<Map<String, Object>> geometryFuture =
                multithreadingManager.executeAsync(() -> generate3DGeometry(dataPoints));

            CompletableFuture<Map<String, Object>> materialsFuture =
                multithreadingManager.executeAsync(() ->
                    mbtiStyleService.generateClimateMaterials(mbtiType, dataPoints));

            // Wait for both to complete
            Map<String, Object> geometry = geometryFuture.get();
            Map<String, Object> materials = materialsFuture.get();

            // Build Three.js compatible visualization data
            Map<String, Object> visualizationData = buildThreeJsScene(
                geometry, materials, dataPoints);

            // Save visualization
            Visualization visualization = saveVisualization(
                userId,
                "Climate Map - " + latitude + ", " + longitude,
                Visualization.VisualizationType.CLIMATE_MAP_3D,
                mbtiType,
                visualizationData,
                System.currentTimeMillis() - startTime
            );

            log.info("Climate map generated successfully in {}ms",
                     System.currentTimeMillis() - startTime);

            return Map.of(
                "visualizationId", visualization.getId(),
                "data", visualizationData,
                "processingTime", System.currentTimeMillis() - startTime,
                "dataPoints", dataPoints.size()
            );

        } catch (Exception e) {
            log.error("Error generating climate map", e);
            throw new RuntimeException("Failed to generate climate map: " + e.getMessage(), e);
        }
    }

    /**
     * Generate Health Risk Heatmap
     */
    @Transactional
    public Map<String, Object> generateHealthHeatmap(
            String userId,
            Double latitude,
            Double longitude,
            Double radius,
            List<String> healthFactors,
            String mbtiType,
            String quality) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting health heatmap generation for location: ({}, {}), radius: {}km",
                     latitude, longitude, radius);

            // Validate inputs
            validateCoordinates(latitude, longitude);
            validateMbtiType(mbtiType);

            // Generate health risk data points
            List<HealthRiskPoint> riskPoints = generateHealthRiskPoints(
                latitude, longitude, radius, healthFactors, quality);

            // Process heatmap in parallel
            CompletableFuture<Map<String, Object>> heatmapFuture =
                multithreadingManager.executeAsync(() -> generateHeatmapGrid(riskPoints));

            CompletableFuture<Map<String, Object>> coloringFuture =
                multithreadingManager.executeAsync(() ->
                    mbtiStyleService.generateHealthHeatmapColors(mbtiType, riskPoints));

            Map<String, Object> heatmapGrid = heatmapFuture.get();
            Map<String, Object> coloring = coloringFuture.get();

            // Build heatmap visualization
            Map<String, Object> visualizationData = buildHeatmapVisualization(
                heatmapGrid, coloring, riskPoints);

            // Save visualization
            Visualization visualization = saveVisualization(
                userId,
                "Health Heatmap - " + latitude + ", " + longitude,
                Visualization.VisualizationType.HEALTH_HEATMAP,
                mbtiType,
                visualizationData,
                System.currentTimeMillis() - startTime
            );

            log.info("Health heatmap generated successfully in {}ms",
                     System.currentTimeMillis() - startTime);

            return Map.of(
                "visualizationId", visualization.getId(),
                "data", visualizationData,
                "processingTime", System.currentTimeMillis() - startTime,
                "riskPoints", riskPoints.size()
            );

        } catch (Exception e) {
            log.error("Error generating health heatmap", e);
            throw new RuntimeException("Failed to generate health heatmap: " + e.getMessage(), e);
        }
    }

    /**
     * Get user visualizations with optional type filter
     */
    @Cacheable(value = "userVisualizations", key = "#userId + '_' + #type")
    public List<Visualization> getUserVisualizations(String userId, String type) {
        log.info("Fetching visualizations for user: {}, type: {}", userId, type);

        if (type != null && !type.isEmpty()) {
            return visualizationRepository.findByUserIdAndVisualizationType(
                userId, Visualization.VisualizationType.valueOf(type));
        }

        return visualizationRepository.findByUserId(userId);
    }

    /**
     * Get specific visualization by ID
     */
    @Cacheable(value = "visualizations", key = "#id")
    public Optional<Visualization> getVisualization(Long id) {
        return visualizationRepository.findById(id);
    }

    /**
     * Delete visualization
     */
    @Transactional
    public void deleteVisualization(Long id) {
        log.info("Deleting visualization with ID: {}", id);
        visualizationRepository.deleteById(id);
    }

    // Private helper methods

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    private void validateMbtiType(String mbtiType) {
        if (mbtiType != null && !mbtiStyleService.isValidMbtiType(mbtiType)) {
            throw new IllegalArgumentException("Invalid MBTI type: " + mbtiType);
        }
    }

    private List<ClimateDataPoint> generateClimateDataPoints(
            Double centerLat, Double centerLon, Double radius, String quality) {

        int gridSize = getGridSize(quality);
        List<ClimateDataPoint> points = new ArrayList<>();
        Random random = new Random(42); // Seeded for consistency

        double latStep = (radius / 111.0) / gridSize; // 111km per degree latitude
        double lonStep = (radius / (111.0 * Math.cos(Math.toRadians(centerLat)))) / gridSize;

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                double lat = centerLat - (radius / 111.0) + (i * latStep);
                double lon = centerLon - (radius / (111.0 * Math.cos(Math.toRadians(centerLat)))) + (j * lonStep);

                // Simulate climate data (in production, fetch from real data sources)
                ClimateDataPoint point = new ClimateDataPoint();
                point.latitude = lat;
                point.longitude = lon;
                point.temperature = 15 + random.nextDouble() * 25; // 15-40°C
                point.precipitation = random.nextDouble() * 200; // 0-200mm
                point.humidity = 30 + random.nextDouble() * 60; // 30-90%
                point.windSpeed = random.nextDouble() * 50; // 0-50 km/h
                point.airQualityIndex = random.nextInt(200); // 0-200 AQI
                point.elevation = random.nextDouble() * 1000; // 0-1000m

                points.add(point);
            }
        }

        log.info("Generated {} climate data points", points.size());
        return points;
    }

    private List<HealthRiskPoint> generateHealthRiskPoints(
            Double centerLat, Double centerLon, Double radius,
            List<String> healthFactors, String quality) {

        int gridSize = getGridSize(quality);
        List<HealthRiskPoint> points = new ArrayList<>();
        Random random = new Random(42);

        double latStep = (radius / 111.0) / gridSize;
        double lonStep = (radius / (111.0 * Math.cos(Math.toRadians(centerLat)))) / gridSize;

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                double lat = centerLat - (radius / 111.0) + (i * latStep);
                double lon = centerLon - (radius / (111.0 * Math.cos(Math.toRadians(centerLat)))) + (j * lonStep);

                HealthRiskPoint point = new HealthRiskPoint();
                point.latitude = lat;
                point.longitude = lon;
                point.heatStressRisk = random.nextDouble() * 100;
                point.respiratoryRisk = random.nextDouble() * 100;
                point.cardiovascularRisk = random.nextDouble() * 100;
                point.vectorBorneRisk = random.nextDouble() * 100;
                point.waterborneRisk = random.nextDouble() * 100;

                // Calculate overall risk
                point.overallRisk = (point.heatStressRisk + point.respiratoryRisk +
                                    point.cardiovascularRisk + point.vectorBorneRisk +
                                    point.waterborneRisk) / 5.0;

                points.add(point);
            }
        }

        log.info("Generated {} health risk points", points.size());
        return points;
    }

    private Map<String, Object> generate3DGeometry(List<ClimateDataPoint> dataPoints) {
        Map<String, Object> geometry = new HashMap<>();

        List<Map<String, Double>> vertices = new ArrayList<>();
        List<List<Integer>> faces = new ArrayList<>();
        List<Map<String, Double>> normals = new ArrayList<>();

        // Generate vertices from data points
        for (ClimateDataPoint point : dataPoints) {
            Map<String, Double> vertex = new HashMap<>();
            vertex.put("x", point.longitude);
            vertex.put("y", point.temperature); // Height represents temperature
            vertex.put("z", point.latitude);
            vertices.add(vertex);
        }

        // Generate faces (triangulation)
        int gridSize = (int) Math.sqrt(dataPoints.size());
        for (int i = 0; i < gridSize - 1; i++) {
            for (int j = 0; j < gridSize - 1; j++) {
                int idx = i * gridSize + j;

                // Triangle 1
                faces.add(List.of(idx, idx + 1, idx + gridSize));

                // Triangle 2
                faces.add(List.of(idx + 1, idx + gridSize + 1, idx + gridSize));
            }
        }

        geometry.put("vertices", vertices);
        geometry.put("faces", faces);
        geometry.put("type", "BufferGeometry");

        return geometry;
    }

    private Map<String, Object> generateHeatmapGrid(List<HealthRiskPoint> riskPoints) {
        Map<String, Object> grid = new HashMap<>();

        int gridSize = (int) Math.sqrt(riskPoints.size());
        double[][] riskGrid = new double[gridSize][gridSize];

        for (int i = 0; i < riskPoints.size(); i++) {
            int row = i / gridSize;
            int col = i % gridSize;
            riskGrid[row][col] = riskPoints.get(i).overallRisk;
        }

        grid.put("grid", riskGrid);
        grid.put("width", gridSize);
        grid.put("height", gridSize);
        grid.put("type", "heatmap");

        return grid;
    }

    private Map<String, Object> buildThreeJsScene(
            Map<String, Object> geometry,
            Map<String, Object> materials,
            List<ClimateDataPoint> dataPoints) {

        Map<String, Object> scene = new HashMap<>();

        // Scene metadata
        scene.put("type", "Scene");
        scene.put("metadata", Map.of(
            "version", "4.5",
            "type", "Object",
            "generator", "ClimateHealthMapper"
        ));

        // Geometry
        scene.put("geometries", List.of(geometry));

        // Materials
        scene.put("materials", materials);

        // Objects (meshes)
        List<Map<String, Object>> objects = new ArrayList<>();
        Map<String, Object> mesh = new HashMap<>();
        mesh.put("type", "Mesh");
        mesh.put("geometry", 0); // Reference to geometry index
        mesh.put("material", 0); // Reference to material index
        mesh.put("userData", Map.of(
            "dataPoints", dataPoints.size(),
            "visualizationType", "climate_map_3d"
        ));
        objects.add(mesh);

        scene.put("object", Map.of("children", objects));

        // Camera settings
        scene.put("camera", Map.of(
            "type", "PerspectiveCamera",
            "fov", 60,
            "aspect", 16.0 / 9.0,
            "near", 0.1,
            "far", 10000,
            "position", List.of(0, 100, 200)
        ));

        // Lighting
        scene.put("lights", List.of(
            Map.of("type", "AmbientLight", "color", 0xffffff, "intensity", 0.5),
            Map.of("type", "DirectionalLight", "color", 0xffffff, "intensity", 0.8,
                   "position", List.of(100, 100, 100))
        ));

        return scene;
    }

    private Map<String, Object> buildHeatmapVisualization(
            Map<String, Object> heatmapGrid,
            Map<String, Object> coloring,
            List<HealthRiskPoint> riskPoints) {

        Map<String, Object> visualization = new HashMap<>();

        visualization.put("type", "Heatmap");
        visualization.put("grid", heatmapGrid);
        visualization.put("colorScheme", coloring);
        visualization.put("dataPoints", riskPoints.size());

        // Statistics
        double avgRisk = riskPoints.stream()
            .mapToDouble(p -> p.overallRisk)
            .average()
            .orElse(0.0);

        double maxRisk = riskPoints.stream()
            .mapToDouble(p -> p.overallRisk)
            .max()
            .orElse(0.0);

        visualization.put("statistics", Map.of(
            "averageRisk", avgRisk,
            "maxRisk", maxRisk,
            "riskDistribution", calculateRiskDistribution(riskPoints)
        ));

        return visualization;
    }

    private Map<String, Integer> calculateRiskDistribution(List<HealthRiskPoint> points) {
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("low", 0);
        distribution.put("moderate", 0);
        distribution.put("high", 0);
        distribution.put("veryHigh", 0);
        distribution.put("extreme", 0);

        for (HealthRiskPoint point : points) {
            if (point.overallRisk < 20) distribution.merge("low", 1, Integer::sum);
            else if (point.overallRisk < 40) distribution.merge("moderate", 1, Integer::sum);
            else if (point.overallRisk < 60) distribution.merge("high", 1, Integer::sum);
            else if (point.overallRisk < 80) distribution.merge("veryHigh", 1, Integer::sum);
            else distribution.merge("extreme", 1, Integer::sum);
        }

        return distribution;
    }

    private Visualization saveVisualization(
            String userId,
            String title,
            Visualization.VisualizationType type,
            String mbtiType,
            Map<String, Object> data,
            long processingTime) {

        try {
            Visualization visualization = Visualization.builder()
                .userId(userId)
                .title(title)
                .visualizationType(type)
                .mbtiType(mbtiType)
                .visualizationData(objectMapper.writeValueAsString(data))
                .processingTimeMs(processingTime)
                .renderQuality("medium")
                .isPublic(false)
                .build();

            return visualizationRepository.save(visualization);

        } catch (Exception e) {
            log.error("Error saving visualization", e);
            throw new RuntimeException("Failed to save visualization", e);
        }
    }

    private int getGridSize(String quality) {
        return switch (quality.toLowerCase()) {
            case "low" -> 20;
            case "high" -> 100;
            default -> 50; // medium
        };
    }

    // Inner classes for data transfer
    @lombok.Data
    private static class ClimateDataPoint {
        private Double latitude;
        private Double longitude;
        private Double temperature;
        private Double precipitation;
        private Double humidity;
        private Double windSpeed;
        private Integer airQualityIndex;
        private Double elevation;
    }

    @lombok.Data
    private static class HealthRiskPoint {
        private Double latitude;
        private Double longitude;
        private Double heatStressRisk;
        private Double respiratoryRisk;
        private Double cardiovascularRisk;
        private Double vectorBorneRisk;
        private Double waterborneRisk;
        private Double overallRisk;
    }
}
