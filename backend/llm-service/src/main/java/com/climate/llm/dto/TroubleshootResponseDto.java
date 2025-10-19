package com.climate.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for troubleshooting responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TroubleshootResponseDto {

    private boolean success;

    private Long errorId;

    private String severity;

    private List<String> suggestions;

    private List<String> relatedDocs;

    private String estimatedResolutionTime;

    private boolean isRecurring;

    private int similarErrorsCount;

    private String error;

    private LocalDateTime timestamp;
}
