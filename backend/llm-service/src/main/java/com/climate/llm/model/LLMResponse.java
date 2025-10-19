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
 * JPA Entity representing an LLM response
 */
@Entity
@Table(name = "llm_responses", indexes = {
    @Index(name = "idx_query_context_id", columnList = "query_context_id"),
    @Index(name = "idx_llm_provider", columnList = "llm_provider"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_context_id", nullable = false)
    private Long queryContextId;

    @Column(name = "llm_provider", nullable = false)
    private String llmProvider;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "response_text", columnDefinition = "TEXT", nullable = false)
    private String responseText;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "cached")
    private Boolean cached;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
