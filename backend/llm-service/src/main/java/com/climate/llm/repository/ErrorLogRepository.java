package com.climate.llm.repository;

import com.climate.llm.model.ErrorLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ErrorLog entity
 */
@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {

    /**
     * Find error logs by error type
     */
    List<ErrorLog> findByErrorType(String errorType, Pageable pageable);

    /**
     * Find error logs by service name
     */
    List<ErrorLog> findByServiceName(String serviceName, Pageable pageable);

    /**
     * Find error logs by error type and service name
     */
    List<ErrorLog> findByErrorTypeAndServiceName(String errorType, String serviceName);

    /**
     * Find error logs by severity
     */
    List<ErrorLog> findBySeverity(String severity, Pageable pageable);

    /**
     * Find unresolved error logs
     */
    List<ErrorLog> findByResolvedFalse(Pageable pageable);

    /**
     * Find error logs created after a specific date
     */
    List<ErrorLog> findByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * Find error logs by user ID
     */
    List<ErrorLog> findByUserId(String userId, Pageable pageable);

    /**
     * Count unresolved errors by service
     */
    @Query("SELECT COUNT(e) FROM ErrorLog e WHERE e.serviceName = :serviceName AND e.resolved = false")
    long countUnresolvedByService(@Param("serviceName") String serviceName);

    /**
     * Find critical errors
     */
    @Query("SELECT e FROM ErrorLog e WHERE e.severity = 'CRITICAL' AND e.resolved = false ORDER BY e.createdAt DESC")
    List<ErrorLog> findCriticalErrors(Pageable pageable);

    /**
     * Get error statistics by type
     */
    @Query("SELECT e.errorType, COUNT(e) FROM ErrorLog e GROUP BY e.errorType ORDER BY COUNT(e) DESC")
    List<Object[]> getErrorStatsByType();

    /**
     * Get error statistics by service
     */
    @Query("SELECT e.serviceName, COUNT(e) FROM ErrorLog e GROUP BY e.serviceName ORDER BY COUNT(e) DESC")
    List<Object[]> getErrorStatsByService();
}
