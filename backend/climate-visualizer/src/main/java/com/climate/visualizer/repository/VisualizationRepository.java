package com.climate.visualizer.repository;

import com.climate.visualizer.model.Visualization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Repository for Visualization entities
 *
 * Provides data access methods for visualizations with
 * custom queries for filtering and retrieval.
 */
@Repository
public interface VisualizationRepository extends JpaRepository<Visualization, Long> {

    /**
     * Find all visualizations for a specific user
     */
    List<Visualization> findByUserId(String userId);

    /**
     * Find visualizations by user and type
     */
    List<Visualization> findByUserIdAndVisualizationType(
        String userId,
        Visualization.VisualizationType visualizationType);

    /**
     * Find public visualizations
     */
    List<Visualization> findByIsPublicTrue();

    /**
     * Find visualizations created after a specific date
     */
    List<Visualization> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find visualizations by MBTI type
     */
    List<Visualization> findByMbtiType(String mbtiType);

    /**
     * Find recent visualizations for a user
     */
    @Query("SELECT v FROM Visualization v WHERE v.userId = :userId ORDER BY v.createdAt DESC")
    List<Visualization> findRecentByUserId(@Param("userId") String userId);

    /**
     * Find visualizations shared with a specific user
     */
    @Query("SELECT v FROM Visualization v WHERE :userId MEMBER OF v.sharedWith")
    List<Visualization> findSharedWithUser(@Param("userId") String userId);

    /**
     * Count visualizations by type
     */
    long countByVisualizationType(Visualization.VisualizationType visualizationType);

    /**
     * Delete old visualizations (older than specified date)
     */
    void deleteByCreatedAtBefore(LocalDateTime date);
}
