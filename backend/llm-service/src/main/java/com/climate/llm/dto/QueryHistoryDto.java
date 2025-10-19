package com.climate.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for query history
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryHistoryDto {

    private Long id;

    private String queryText;

    private String mbtiType;

    private String location;

    private LocalDateTime createdAt;
}
