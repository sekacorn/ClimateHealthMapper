package com.climate.integrator.repository;

import com.climate.integrator.model.HealthData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Repository for Health Data (FHIR)
 */
@Repository
public interface HealthDataRepository extends JpaRepository<HealthData, Long> {

    /**
     * Find all health data for a specific user
     */
    List<HealthData> findByUserId(String userId);

    /**
     * Find health data for a user within a date range
     */
    List<HealthData> findByUserIdAndObservationDateBetween(
        String userId,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    /**
     * Find health data by resource type
     */
    List<HealthData> findByUserIdAndResourceType(String userId, String resourceType);

    /**
     * Find health data by category
     */
    List<HealthData> findByUserIdAndCategory(String userId, String category);

    /**
     * Find health data by code
     */
    List<HealthData> findByUserIdAndCode(String userId, String code);

    /**
     * Find observations with abnormal values
     */
    @Query("SELECT h FROM HealthData h WHERE h.userId = :userId " +
           "AND h.resourceType = 'Observation' " +
           "AND h.interpretation IN ('H', 'L', 'A', 'AA')")
    List<HealthData> findAbnormalObservations(@Param("userId") String userId);

    /**
     * Find active conditions
     */
    @Query("SELECT h FROM HealthData h WHERE h.userId = :userId " +
           "AND h.resourceType = 'Condition' " +
           "AND h.clinicalStatus = 'active'")
    List<HealthData> findActiveConditions(@Param("userId") String userId);

    /**
     * Find current medications
     */
    @Query("SELECT h FROM HealthData h WHERE h.userId = :userId " +
           "AND h.resourceType = 'MedicationRequest' " +
           "AND h.clinicalStatus = 'active'")
    List<HealthData> findCurrentMedications(@Param("userId") String userId);

    /**
     * Get latest health data for a user
     */
    @Query("SELECT h FROM HealthData h WHERE h.userId = :userId " +
           "ORDER BY h.observationDate DESC")
    List<HealthData> findLatestByUserId(@Param("userId") String userId);
}
