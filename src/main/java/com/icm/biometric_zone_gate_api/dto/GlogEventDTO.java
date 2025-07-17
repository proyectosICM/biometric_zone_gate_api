package com.icm.biometric_zone_gate_api.dto;

import lombok.Data;

// Eventos en tiempo real
@Data
public class GlogEventDTO {
    private String userId;
    private String time;
    private String verifyMode;
    private int ioMode;
    private String inOut;
    private String doorMode;
    private String logPhoto;
}
