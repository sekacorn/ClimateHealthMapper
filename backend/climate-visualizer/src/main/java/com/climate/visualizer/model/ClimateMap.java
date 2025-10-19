package com.climate.visualizer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * JPA Entity for 3D Climate Visualizations
 *
 * Stores climate map data including temperature, precipitation,
 * humidity, and other climate parameters for 3D rendering.
 */
@Entity
@Table(name = "climate_maps", indexes = {
    @Index(name = "idx_climate_map_location", columnList = "latitude,longitude"),
    @Index(name = "idx_climate_map_date", columnList = "dataDate")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClimateMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private LocalDateTime dataDate;

    // Climate parameters
    @Column(name = "temperature_celsius")
    private Double temperatureCelsius;

    @Column(name = "precipitation_mm")
    private Double precipitationMm;

    @Column(name = "humidity_percent")
    private Double humidityPercent;

    @Column(name = "wind_speed_kmh")
    private Double windSpeedKmh;

    @Column(name = "air_quality_index")
    private Integer airQualityIndex;

    @Column(name = "uv_index")
    private Double uvIndex;

    @Column(name = "pressure_hpa")
    private Double pressureHpa;

    // 3D visualization data (stored as JSON)
    @Column(columnDefinition = "TEXT")
    private String meshData;

    @Column(columnDefinition = "TEXT")
    private String geometryData;

    // Additional metadata
    @ElementCollection
    @CollectionTable(name = "climate_map_metadata",
                     joinColumns = @JoinColumn(name = "climate_map_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, String> metadata;

    @Column(name = "elevation_meters")
    private Double elevationMeters;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
