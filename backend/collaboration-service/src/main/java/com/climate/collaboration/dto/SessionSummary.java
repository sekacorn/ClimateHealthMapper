package com.climate.collaboration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionSummary {

    private String sessionId;
    private String sessionName;
    private Integer participantCount;
    private Integer maxParticipants;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
    private Boolean isPublic;
}
