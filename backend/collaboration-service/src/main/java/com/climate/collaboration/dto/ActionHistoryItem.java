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
public class ActionHistoryItem {

    private String userId;
    private String userName;
    private String actionType;
    private LocalDateTime timestamp;
    private String mbtiType;
}
