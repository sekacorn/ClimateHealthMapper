package com.climate.llm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for troubleshooting requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TroubleshootRequestDto {

    @NotBlank(message = "Error type is required")
    private String errorType;

    @NotBlank(message = "Service name is required")
    private String serviceName;

    @NotBlank(message = "Error message is required")
    private String errorMessage;

    private String stackTrace;

    private String userId;

    private String sessionId;

    private String metadata;
}
