package com.icm.biometric_zone_gate_api.dto;

public class DeviceAuthDTO {
    private String dev_id;       // ID único del dispositivo
    private String dev_model;    // Modelo del dispositivo
    private String token;      // MD5(dev_id + clave)
}
