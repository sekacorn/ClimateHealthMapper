package com.climate.llm.repository;

import com.climate.llm.model.QueryContext;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for QueryContext entity
 */
@Repository
public interface QueryContextRepository extends JpaRepository<QueryContext, Long> {

    /**
     * Find query contexts by user ID
     */
    List<QueryContext> findByUserId(String userId, Pageable pageable);

    /**
     * Find query contexts by MBTI type
     */
    List<QueryContext> findByMbtiType(String mbtiType, Pageable pageable);

    /**
     * Find query contexts by user ID and MBTI type
     */
    List<QueryContext> findByUserIdAndMbtiType(String userId, String mbtiType, Pageable pageable);

    /**
     * Find query contexts created after a specific date
     */
    List<QueryContext> findByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * Find query contexts by session ID
     */
    List<QueryContext> findBySessionId(String sessionId);

    /**
     * Count queries by user ID
     */
    long countByUserId(String userId);

    /**
     * Count queries by MBTI type
     */
    long countByMbtiType(String mbtiType);

    /**
     * Find most recent query contexts
     */
    @Query("SELECT qc FROM QueryContext qc ORDER BY qc.createdAt DESC")
    List<QueryContext> findRecentQueries(Pageable pageable);

    /**
     * Search query contexts by text
     */
    @Query("SELECT qc FROM QueryContext qc WHERE LOWER(qc.queryText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    List<QueryContext> searchByQueryText(@Param("searchText") String searchText, Pageable pageable);
}
