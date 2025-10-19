package com.climate.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaEnableResponse {

    private String secret;

    private String qrCodeUrl;

    private List<String> backupCodes;

    private String message;
}
