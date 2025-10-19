package com.climate.integrator.repository;

import com.climate.integrator.model.EnvData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Repository for Environmental Data
 */
@Repository
public interface EnvDataRepository extends JpaRepository<EnvData, Long> {

    /**
     * Find all environmental data for a specific user
     */
    List<EnvData> findByUserId(String userId);

    /**
     * Find environmental data for a user within a date range
     */
    List<EnvData> findByUserIdAndMeasurementDateBetween(
        String userId,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    /**
     * Find environmental data by data source
     */
    List<EnvData> findByUserIdAndDataSource(String userId, String dataSource);

    /**
     * Find environmental data within a geographic bounding box
     */
    @Query("SELECT e FROM EnvData e WHERE e.userId = :userId " +
           "AND e.latitude BETWEEN :minLat AND :maxLat " +
           "AND e.longitude BETWEEN :minLon AND :maxLon")
    List<EnvData> findByUserIdAndLocationBounds(
        @Param("userId") String userId,
        @Param("minLat") Double minLat,
        @Param("maxLat") Double maxLat,
        @Param("minLon") Double minLon,
        @Param("maxLon") Double maxLon
    );

    /**
     * Find environmental data with high AQI
     */
    List<EnvData> findByUserIdAndAqiGreaterThanEqual(String userId, Double aqiThreshold);

    /**
     * Get latest environmental data for a user
     */
    @Query("SELECT e FROM EnvData e WHERE e.userId = :userId " +
           "ORDER BY e.measurementDate DESC")
    List<EnvData> findLatestByUserId(@Param("userId") String userId);

    /**
     * Delete old environmental data
     */
    void deleteByMeasurementDateBefore(LocalDateTime date);
}
