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
public class SessionResponse {

    private String sessionId;
    private String sessionName;
    private String creatorUserId;
    private LocalDateTime createdAt;
    private String status;
    private Integer participantCount;
    private Integer maxParticipants;
    private Boolean isPublic;
}
