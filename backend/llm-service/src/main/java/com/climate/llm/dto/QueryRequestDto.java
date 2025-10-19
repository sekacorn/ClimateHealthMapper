package com.climate.llm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for LLM query requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequestDto {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Query text is required")
    private String queryText;

    @Pattern(regexp = "^(INTJ|INTP|ENTJ|ENTP|INFJ|INFP|ENFJ|ENFP|ISTJ|ISFJ|ESTJ|ESFJ|ISTP|ISFP|ESTP|ESFP)$",
             message = "Invalid MBTI type")
    private String mbtiType;

    private String location;

    private String sessionId;

    private String preferredProvider; // huggingface, openai, xai

    private String contextData;
}
