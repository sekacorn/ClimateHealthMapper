package com.climate.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for LLM query responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponseDto {

    private boolean success;

    private Long queryId;

    private String responseText;

    private String mbtiType;

    private String provider;

    private Long processingTimeMs;

    private Boolean cached;

    private String error;

    private LocalDateTime timestamp;
}
