package com.climate.collaboration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSessionRequest {

    private String sessionName;
    private Integer maxParticipants;
    private Boolean isPublic;
}
