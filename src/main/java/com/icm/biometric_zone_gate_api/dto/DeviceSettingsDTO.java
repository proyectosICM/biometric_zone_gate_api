package com.icm.biometric_zone_gate_api.dto;

import lombok.Data;

@Data
public class DeviceSettingsDTO {
    private Integer deviceid;     // opcional
    private Integer language;
    private Integer volume;       // 0..10
    private Integer screensaver;  // 0..255
    private Integer verifymode;   // enum protocolo
    private Integer sleep;        // 0/1 o true/false → aquí lo dejamos 0/1
    private Integer userfpnum;    // 1..10
    private Integer loghint;      // 0 o umbral
    private Integer reverifytime; // 0..255
}