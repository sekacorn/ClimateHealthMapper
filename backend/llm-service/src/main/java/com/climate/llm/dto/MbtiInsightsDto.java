package com.climate.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for MBTI personality insights
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MbtiInsightsDto {

    private String mbtiType;

    private String name;

    private String style;

    private String characteristics;

    private String preferences;

    private String communicationTips;
}
