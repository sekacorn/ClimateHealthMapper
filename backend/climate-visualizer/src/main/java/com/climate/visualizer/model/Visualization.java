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
 * JPA Entity for Saved Visualizations
 *
 * Stores user-generated visualizations with MBTI styling preferences
 * and export metadata.
 */
@Entity
@Table(name = "visualizations", indexes = {
    @Index(name = "idx_visualization_user", columnList = "userId"),
    @Index(name = "idx_visualization_type", columnList = "visualizationType"),
    @Index(name = "idx_visualization_created", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Visualization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "visualization_type", nullable = false)
    private VisualizationType visualizationType;

    // MBTI styling
    @Column(name = "mbti_type")
    private String mbtiType;

    @Column(columnDefinition = "TEXT")
    private String styleConfiguration;

    // Visualization data (Three.js compatible JSON)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String visualizationData;

    @Column(columnDefinition = "TEXT")
    private String cameraSettings;

    @Column(columnDefinition = "TEXT")
    private String lightingSettings;

    // Linked data
    @Column(name = "climate_map_id")
    private Long climateMapId;

    @Column(name = "health_map_id")
    private Long healthMapId;

    // Export information
    @ElementCollection
    @CollectionTable(name = "visualization_exports",
                     joinColumns = @JoinColumn(name = "visualization_id"))
    @MapKeyColumn(name = "export_format")
    @Column(name = "export_path")
    private Map<String, String> exportPaths;

    @Column(name = "last_exported_at")
    private LocalDateTime lastExportedAt;

    // Processing metadata
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "render_quality")
    private String renderQuality;

    // Sharing and visibility
    @Column(name = "is_public")
    private Boolean isPublic;

    @Column(name = "shared_with")
    @ElementCollection
    @CollectionTable(name = "visualization_shares",
                     joinColumns = @JoinColumn(name = "visualization_id"))
    private java.util.List<String> sharedWith;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isPublic == null) {
            isPublic = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Visualization Type Enumeration
     */
    public enum VisualizationType {
        CLIMATE_MAP_3D,
        HEALTH_HEATMAP,
        COMBINED_OVERLAY,
        TEMPORAL_ANIMATION,
        COMPARATIVE_SPLIT
    }
}
