package com.icm.biometric_zone_gate_api.dto;

import lombok.Data;

@Data
public class DeviceDTO {
    private Long id;
    private String model;
    private String firmwareVersion;
    private String name;
    private Boolean activo;
    // token y lastSeen
    private DeviceSettingDTO setting;
}
