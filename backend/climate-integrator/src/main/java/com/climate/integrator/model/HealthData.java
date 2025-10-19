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
 * JPA Entity for Health Data (FHIR)
 *
 * Stores parsed FHIR health resources including observations,
 * conditions, medications, and procedures
 */
@Entity
@Table(name = "health_data", indexes = {
    @Index(name = "idx_user_id_health", columnList = "userId"),
    @Index(name = "idx_fhir_resource_type", columnList = "resourceType"),
    @Index(name = "idx_observation_date", columnList = "observationDate")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "User ID is required")
    @Column(nullable = false)
    private String userId;

    @NotBlank(message = "FHIR resource type is required")
    @Column(nullable = false)
    private String resourceType; // Observation, Condition, MedicationRequest, etc.

    private String fhirResourceId; // Original FHIR resource ID

    @NotNull(message = "Observation date is required")
    @Column(nullable = false)
    private LocalDateTime observationDate;

    // Common FHIR fields
    private String code; // LOINC, SNOMED, etc.
    private String codeSystem;
    private String displayName;

    // Value fields (for Observations)
    private String valueType; // Quantity, CodeableConcept, String, etc.
    private Double valueQuantity;
    private String valueUnit;
    private String valueCode;
    private String valueString;

    // Clinical interpretation
    private String interpretation; // Normal, High, Low, etc.
    private String category; // vital-signs, laboratory, etc.

    // Reference ranges
    private Double referenceLow;
    private Double referenceHigh;

    // Condition-specific fields
    private String clinicalStatus; // active, resolved, etc.
    private String verificationStatus;
    private String severity;

    // Medication-specific fields
    private String medicationCode;
    private String dosage;
    private String frequency;

    // Performer/Practitioner
    private String practitionerId;
    private String practitionerName;

    // Location
    private String facilityId;
    private String facilityName;

    @Column(columnDefinition = "TEXT")
    private String rawFhirJson; // Store complete FHIR JSON

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
