package com.climate.integrator.repository;

import com.climate.integrator.model.GenomicData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA Repository for Genomic Data (VCF)
 */
@Repository
public interface GenomicDataRepository extends JpaRepository<GenomicData, Long> {

    /**
     * Find all genomic data for a specific user
     */
    List<GenomicData> findByUserId(String userId);

    /**
     * Find variants by chromosome
     */
    List<GenomicData> findByUserIdAndChromosome(String userId, String chromosome);

    /**
     * Find variants by gene name
     */
    List<GenomicData> findByUserIdAndGeneName(String userId, String geneName);

    /**
     * Find variants by rsID
     */
    List<GenomicData> findByUserIdAndRsId(String userId, String rsId);

    /**
     * Find climate-relevant variants
     */
    List<GenomicData> findByUserIdAndClimateRelevant(String userId, Boolean climateRelevant);

    /**
     * Find variants by clinical significance
     */
    List<GenomicData> findByUserIdAndClinicalSignificance(
        String userId,
        String clinicalSignificance
    );

    /**
     * Find high-impact variants
     */
    @Query("SELECT g FROM GenomicData g WHERE g.userId = :userId " +
           "AND g.impact IN ('HIGH', 'MODERATE')")
    List<GenomicData> findHighImpactVariants(@Param("userId") String userId);

    /**
     * Find variants in a genomic region
     */
    @Query("SELECT g FROM GenomicData g WHERE g.userId = :userId " +
           "AND g.chromosome = :chromosome " +
           "AND g.position BETWEEN :startPos AND :endPos")
    List<GenomicData> findByUserIdAndGenomicRegion(
        @Param("userId") String userId,
        @Param("chromosome") String chromosome,
        @Param("startPos") Long startPos,
        @Param("endPos") Long endPos
    );

    /**
     * Find pathogenic variants
     */
    @Query("SELECT g FROM GenomicData g WHERE g.userId = :userId " +
           "AND g.clinicalSignificance LIKE '%pathogenic%'")
    List<GenomicData> findPathogenicVariants(@Param("userId") String userId);

    /**
     * Count climate-relevant variants for a user
     */
    Long countByUserIdAndClimateRelevant(String userId, Boolean climateRelevant);
}
