package com.climate.collaboration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketMessage {

    private String action;
    private String userId;
    private String userName;
    private String mbtiType;
    private Object data;
    private LocalDateTime timestamp;
    private String messageType;
    private Map<String, Object> mbtiEnhancements;
}
