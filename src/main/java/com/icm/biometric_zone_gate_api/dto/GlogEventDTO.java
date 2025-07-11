package com.icm.biometric_zone_gate_api.dto;

import lombok.Data;

@Data
public class GlogEventDTO {
    private String userId;
    private String tiempo;
    private String verifyMode;
    private int ioMode;
    private String inOut;
    private String doorMode;
    private String logPhoto;
}
