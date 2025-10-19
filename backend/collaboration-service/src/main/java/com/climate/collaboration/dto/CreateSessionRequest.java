package com.climate.collaboration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSessionRequest {

    @NotBlank(message = "Session name is required")
    private String sessionName;

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "User name is required")
    private String userName;

    @NotBlank(message = "MBTI type is required")
    private String mbtiType;

    private Integer maxParticipants;

    @NotNull(message = "isPublic flag is required")
    private Boolean isPublic;
}
