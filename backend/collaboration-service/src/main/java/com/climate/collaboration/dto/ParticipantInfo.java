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
public class ParticipantInfo {

    private String userId;
    private String userName;
    private String mbtiType;
    private String role;
    private String color;
    private LocalDateTime joinedAt;
    private LocalDateTime lastActiveAt;
}
