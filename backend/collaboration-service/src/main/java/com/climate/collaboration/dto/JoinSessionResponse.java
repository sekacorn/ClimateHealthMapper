package com.climate.collaboration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinSessionResponse {

    private String sessionId;
    private Long participantId;
    private String assignedColor;
    private String role;
    private Integer currentParticipants;
    private String sessionSnapshot;
    private String sharedFilters;
}
