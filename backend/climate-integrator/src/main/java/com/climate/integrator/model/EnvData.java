package com.climate.integrator.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * JPA Entity for Environmental Data
 *
 * Stores environmental measurements from NOAA, EPA, and other sources
 * including temperature, air quality, precipitation, etc.
 */
@Entity
@Table(name = "environmental_data", indexes = {
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_location", columnList = "latitude, longitude"),
    @Index(name = "idx_measurement_date", columnList = "measurementDate")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "User ID is required")
    @Column(nullable = false)
    private String userId;

    @NotBlank(message = "Data source is required")
    @Column(nullable = false)
    private String dataSource; // NOAA, EPA, etc.

    @NotNull(message = "Measurement date is required")
    @Column(nullable = false)
    private LocalDateTime measurementDate;

    @NotNull(message = "Latitude is required")
    @Column(nullable = false)
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @Column(nullable = false)
    private Double longitude;

    private String location; // City, state, or region name

    // Temperature data
    private Double temperature; // Celsius
    private Double temperatureMin;
    private Double temperatureMax;

    // Air quality data
    private Double aqi; // Air Quality Index
    private Double pm25; // PM2.5 particulate matter
    private Double pm10; // PM10 particulate matter
    private Double ozone; // O3
    private Double no2; // Nitrogen dioxide
    private Double so2; // Sulfur dioxide
    private Double co; // Carbon monoxide

    // Precipitation data
    private Double precipitation; // mm
    private Double humidity; // percentage

    // Wind data
    private Double windSpeed; // km/h
    private Double windDirection; // degrees

    // Additional environmental factors
    private Double uvIndex;
    private Double pressure; // hPa

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON for additional data

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
