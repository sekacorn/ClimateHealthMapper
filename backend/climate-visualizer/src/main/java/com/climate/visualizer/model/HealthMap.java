package com.climate.visualizer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Entity for Health Risk Heatmaps
 *
 * Stores health risk assessment data for heatmap visualization
 * based on climate conditions and health factors.
 */
@Entity
@Table(name = "health_maps", indexes = {
    @Index(name = "idx_health_map_location", columnList = "latitude,longitude"),
    @Index(name = "idx_health_map_risk", columnList = "overallRiskScore")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private String region;

    // Health risk scores (0-100)
    @Column(name = "heat_stress_risk")
    private Double heatStressRisk;

    @Column(name = "respiratory_risk")
    private Double respiratoryRisk;

    @Column(name = "cardiovascular_risk")
    private Double cardiovascularRisk;

    @Column(name = "vector_borne_disease_risk")
    private Double vectorBorneDiseaseRisk;

    @Column(name = "waterborne_disease_risk")
    private Double waterborneDiseaseRisk;

    @Column(name = "overall_risk_score")
    private Double overallRiskScore;

    // Risk category
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category")
    private RiskCategory riskCategory;

    // Heatmap visualization data
    @Column(columnDefinition = "TEXT")
    private String heatmapData;

    @Column(columnDefinition = "TEXT")
    private String colorGradient;

    // Contributing factors
    @ElementCollection
    @CollectionTable(name = "health_map_factors",
                     joinColumns = @JoinColumn(name = "health_map_id"))
    @Column(name = "factor")
    private List<String> contributingFactors;

    @Column(name = "population_density")
    private Integer populationDensity;

    @Column(name = "vulnerable_population_percent")
    private Double vulnerablePopulationPercent;

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

    /**
     * Risk Category Enumeration
     */
    public enum RiskCategory {
        LOW,
        MODERATE,
        HIGH,
        VERY_HIGH,
        EXTREME
    }
}
