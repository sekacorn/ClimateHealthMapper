package com.climate.llm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * JPA Entity for logging errors and troubleshooting
 */
@Entity
@Table(name = "error_logs", indexes = {
    @Index(name = "idx_error_type", columnList = "error_type"),
    @Index(name = "idx_service_name", columnList = "service_name"),
    @Index(name = "idx_severity", columnList = "severity"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "error_type", nullable = false)
    private String errorType;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "severity", nullable = false)
    private String severity; // ERROR, WARNING, CRITICAL

    @Column(name = "error_message", columnDefinition = "TEXT", nullable = false)
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "troubleshooting_suggestion", columnDefinition = "TEXT")
    private String troubleshootingSuggestion;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "resolved")
    private Boolean resolved;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
